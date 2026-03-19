package com.aicodereview.review_job_service.service;

import com.aicodereview.review_job_service.entity.*;
import com.aicodereview.review_job_service.model.PullRequestEvent;
import com.aicodereview.review_job_service.model.ReviewComment;
import com.aicodereview.review_job_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewStorageService {

    private final PullRequestRepository prRepository;
    private final ReviewCommentRepository commentRepository;
    private final ReviewSummaryRepository summaryRepository;

    @Transactional
    public PullRequestEntity savePullRequest(PullRequestEvent event) {
        String repoFullName = event.getRepository().getFullName();
        int prNumber = event.getPullRequest().getNumber();

        // Upsert — update if exists, create if not
        PullRequestEntity entity = prRepository
            .findByRepoFullNameAndPrNumber(repoFullName, prNumber)
            .orElse(new PullRequestEntity());

        entity.setRepoFullName(repoFullName);
        entity.setPrNumber(prNumber);
        entity.setCommitSha(event.getPullRequest().getHead().getSha());
        entity.setStatus("reviewing");

        PullRequestEntity saved = prRepository.save(entity);
        log.info("Saved PR record: {}", saved.getId());
        return saved;
    }

    @Transactional
    public void saveReviewResults(
            PullRequestEntity pr,
            List<ReviewComment> comments,
            long durationMs) {

        // Save each comment
        for (ReviewComment c : comments) {
            ReviewCommentEntity entity = new ReviewCommentEntity();
            entity.setPrId(pr.getId());
            entity.setFilePath(c.getFile());
            entity.setLineNumber(c.getLine());
            entity.setSeverity(c.getSeverity());
            entity.setCategory(c.getCategory());
            entity.setComment(c.getComment());
            entity.setSuggestion(c.getSuggestion());
            commentRepository.save(entity);
        }

        // Save summary
        ReviewSummaryEntity summary = new ReviewSummaryEntity();
        summary.setPrId(pr.getId());
        summary.setTotalIssues(comments.size());
        summary.setCriticalCount((int) comments.stream()
            .filter(c -> "CRITICAL".equals(c.getSeverity())).count());
        summary.setWarningCount((int) comments.stream()
            .filter(c -> "WARNING".equals(c.getSeverity())).count());
        summary.setInfoCount((int) comments.stream()
            .filter(c -> "INFO".equals(c.getSeverity())).count());
        summary.setReviewDurationMs((int) durationMs);
        summary.setAiModel("groq-llama3");
        summaryRepository.save(summary);

        // Update PR status to completed
        pr.setStatus("completed");
        pr.setReviewedAt(Instant.now());
        prRepository.save(pr);

        log.info("✅ Saved review results — {} comments, {}ms",
            comments.size(), durationMs);
    }

    @Transactional
    public void markFailed(PullRequestEntity pr) {
        pr.setStatus("failed");
        pr.setReviewedAt(Instant.now());
        prRepository.save(pr);
    }
}