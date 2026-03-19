package com.aicodereview.review_job_service.consumer;

import com.aicodereview.review_job_service.ai.AiReviewService;
import com.aicodereview.review_job_service.ai.DiffChunkingService;
import com.aicodereview.review_job_service.entity.PullRequestEntity;
import com.aicodereview.review_job_service.github.DiffParser;
import com.aicodereview.review_job_service.github.GitHubApiClient;
import com.aicodereview.review_job_service.github.GitHubCommentService;
import com.aicodereview.review_job_service.model.FileDiff;
import com.aicodereview.review_job_service.model.PullRequestEvent;
import com.aicodereview.review_job_service.model.ReviewChunk;
import com.aicodereview.review_job_service.model.ReviewComment;
import com.aicodereview.review_job_service.service.DeadLetterService;
import com.aicodereview.review_job_service.service.RedisLockService;
import com.aicodereview.review_job_service.service.RetryService;
import com.aicodereview.review_job_service.service.ReviewStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.aicodereview.review_job_service.service.SlackNotificationService;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewJobConsumer {

    private final GitHubApiClient gitHubApiClient;
    private final DiffParser diffParser;
    private final DiffChunkingService chunkingService;
    private final AiReviewService aiReviewService;
    private final GitHubCommentService commentService;
    private final RedisLockService lockService;
    private final RetryService retryService;
    private final DeadLetterService deadLetterService;
    private final ReviewStorageService storageService;
    private final ObjectMapper objectMapper;
    private final SlackNotificationService slackService;

    @KafkaListener(
        topics = "pr.received",
        groupId = "review-job-service",
        concurrency = "3"
    )
    public void handlePrReceived(
            @Payload String message,
            Acknowledgment ack) {

        String prId = null;
        PullRequestEntity prEntity = null;

        try {
            PullRequestEvent event = objectMapper.readValue(
                message, PullRequestEvent.class
            );

            String repoFullName = event.getRepository().getFullName();
            int prNumber = event.getPullRequest().getNumber();
            String commitSha = event.getPullRequest().getHead().getSha();
            String action = event.getAction();
            prId = repoFullName + "#" + prNumber;

            log.info("Processing PR {} (action: {})", prId, action);

            // Skip non-review actions
            if (!action.equals("opened") && !action.equals("synchronize")) {
                log.info("Skipping action '{}'", action);
                ack.acknowledge();
                return;
            }

            // STEP 1 — Acquire distributed lock
            if (!lockService.tryLock(prId)) {
                log.info("PR {} already being reviewed — skipping", prId);
                ack.acknowledge();
                return;
            }

            long startTime = System.currentTimeMillis();

            try {
                // STEP 2 — Save PR to database
                prEntity = storageService.savePullRequest(event);

                // STEP 3 — Fetch diff with retry
                String rawDiff = retryService.executeWithRetry(
                    () -> gitHubApiClient.getPullRequestDiff(repoFullName, prNumber),
                    "Fetch PR diff"
                );

                if (rawDiff == null || rawDiff.isBlank()) {
                    log.warn("Empty diff for PR {} — skipping", prId);
                    ack.acknowledge();
                    return;
                }

                // STEP 4 — Parse + chunk
                List<FileDiff> fileDiffs = diffParser.parse(rawDiff);
                List<ReviewChunk> chunks = chunkingService.chunk(fileDiffs);
                log.info("Parsed {} files into {} chunks", fileDiffs.size(), chunks.size());

                // STEP 5 — AI review with retry
                List<ReviewComment> allComments = new ArrayList<>();
                for (ReviewChunk chunk : chunks) {
                    List<ReviewComment> comments = retryService.executeWithRetry(
                        () -> aiReviewService.reviewChunk(chunk),
                        "AI review chunk"
                    );
                    allComments.addAll(comments);
                }
                log.info("AI found {} total issues", allComments.size());

                // STEP 6 — Post comments to GitHub with retry
                retryService.executeWithRetry(() -> {
                    commentService.postReviewComments(
                        repoFullName, prNumber, commitSha, allComments
                    );
                    return null;
                }, "Post GitHub comments");

                // STEP 7 — Save results to database
                long duration = System.currentTimeMillis() - startTime;
                storageService.saveReviewResults(prEntity, allComments, duration);
                                // STEP 8 — Send Slack notification
                slackService.sendReviewSummary(
                    repoFullName,
                    prNumber,
                    "PR #" + prNumber,
                    allComments,
                    duration
                );
                log.info("✅ Review complete for PR {} in {}ms", prId, duration);
                ack.acknowledge();

            } finally {
                lockService.releaseLock(prId);
            }

        } catch (Exception e) {
            log.error("❌ PR review failed permanently: {}", e.getMessage());

            if (prEntity != null) {
                storageService.markFailed(prEntity);
            }
            slackService.sendFailureAlert(
                prId != null ? prId.split("#")[0] : "unknown",
                prId != null ? Integer.parseInt(prId.split("#")[1]) : 0,
                e.getMessage()
            );
            deadLetterService.sendToDeadLetter(message, e.getMessage());
            ack.acknowledge();
        }
    }
}