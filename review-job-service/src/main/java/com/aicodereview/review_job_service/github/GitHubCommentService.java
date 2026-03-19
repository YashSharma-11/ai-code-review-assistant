package com.aicodereview.review_job_service.github;

import com.aicodereview.review_job_service.model.ReviewComment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GitHubCommentService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${github.token}")
    private String githubToken;

    public void postReviewComments(
            String repoFullName,
            int prNumber,
            String commitSha,
            List<ReviewComment> comments) {

        if (comments.isEmpty()) {
            log.info("No issues found — posting LGTM comment");
            postLgtmComment(repoFullName, prNumber);
            return;
        }

        // Post as a general PR comment instead of inline
        // to avoid line resolution issues
        postSummaryComment(repoFullName, prNumber, comments);
    }

    private void postSummaryComment(String repoFullName, int prNumber,
            List<ReviewComment> comments) {

        String url = "https://api.github.com/repos/" + repoFullName
                + "/issues/" + prNumber + "/comments";

        StringBuilder body = new StringBuilder();
        body.append(buildSummary(comments)).append("\n\n");
        body.append("### Detailed Issues\n\n");

        for (ReviewComment c : comments) {
            String emoji = switch (c.getSeverity()) {
                case "CRITICAL" -> "🔴";
                case "WARNING"  -> "⚠️";
                default         -> "💡";
            };
            body.append(emoji).append(" **").append(c.getCategory()).append("**");
            body.append(" — `").append(c.getFile()).append("` line ").append(c.getLine()).append("\n");
            body.append("> ").append(c.getComment()).append("\n\n");

            if (c.getSuggestion() != null && !c.getSuggestion().isBlank()) {
                body.append("```java\n").append(c.getSuggestion()).append("\n```\n\n");
            }
        }

        Map<String, String> requestBody = Map.of("body", body.toString());

        try {
            restTemplate.postForEntity(
                url,
                new HttpEntity<>(requestBody, buildHeaders()),
                String.class
            );
            log.info("✅ Posted review summary with {} issues on PR #{}", 
                comments.size(), prNumber);
        } catch (Exception e) {
            log.error("Failed to post summary comment: {}", e.getMessage());
        }
    }
    private void postLgtmComment(String repoFullName, int prNumber) {
        String url = "https://api.github.com/repos/" + repoFullName
                + "/issues/" + prNumber + "/comments";

        Map<String, String> body = Map.of(
            "body", "✅ **AI Code Review**: No issues found. LGTM! 🎉"
        );

        try {
            restTemplate.postForEntity(
                url,
                new HttpEntity<>(body, buildHeaders()),
                String.class
            );
        } catch (Exception e) {
            log.error("Failed to post LGTM comment: {}", e.getMessage());
        }
    }

    private String formatComment(ReviewComment c) {
        String emoji = switch (c.getSeverity()) {
            case "CRITICAL" -> "🔴";
            case "WARNING"  -> "⚠️";
            default         -> "💡";
        };

        StringBuilder sb = new StringBuilder();
        sb.append(emoji).append(" **").append(c.getCategory()).append("**: ");
        sb.append(c.getComment());

        if (c.getSuggestion() != null && !c.getSuggestion().isBlank()) {
            sb.append("\n\n```suggestion\n")
              .append(c.getSuggestion())
              .append("\n```");
        }

        return sb.toString();
    }

    private String buildSummary(List<ReviewComment> comments) {
        long critical = comments.stream()
            .filter(c -> "CRITICAL".equals(c.getSeverity())).count();
        long warning = comments.stream()
            .filter(c -> "WARNING".equals(c.getSeverity())).count();
        long info = comments.stream()
            .filter(c -> "INFO".equals(c.getSeverity())).count();

        return """
            ## 🤖 AI Code Review Summary
            - 🔴 Critical: %d
            - ⚠️ Warning: %d
            - 💡 Info: %d
            - 📝 Total issues: %d
            """.formatted(critical, warning, info, comments.size());
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubToken);
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}