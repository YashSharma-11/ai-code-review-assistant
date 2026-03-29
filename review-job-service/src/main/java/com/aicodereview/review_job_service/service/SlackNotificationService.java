package com.aicodereview.review_job_service.service;

import com.aicodereview.review_job_service.model.ReviewComment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class SlackNotificationService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${slack.webhook-url}")
    private String webhookUrl;

    public void sendReviewSummary(
            String repoFullName,
            int prNumber,
            String prTitle,
            List<ReviewComment> comments,
            long durationMs,
            int qualityScore) {

        long critical = comments.stream()
            .filter(c -> "CRITICAL".equals(c.getSeverity())).count();
        long warning = comments.stream()
            .filter(c -> "WARNING".equals(c.getSeverity())).count();
        long info = comments.stream()
            .filter(c -> "INFO".equals(c.getSeverity())).count();

        String prUrl = "https://github.com/" + repoFullName + "/pull/" + prNumber;
        String scoreEmoji = qualityScore >= 90 ? "✅" :
                            qualityScore >= 75 ? "👍" :
                            qualityScore >= 60 ? "⚠️" : "❌";

        String message = buildSlackMessage(
            repoFullName, prNumber, prUrl,
            comments.size(), critical, warning, info,
            durationMs, qualityScore, scoreEmoji
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.postForEntity(
                webhookUrl,
                new HttpEntity<String>(message, headers),
                String.class
            );
            log.info("✅ Slack notification sent for PR #{}", prNumber);
        } catch (Exception e) {
            log.error("Failed to send Slack notification: {}", e.getMessage());
        }
    }

    public void sendFailureAlert(String repoFullName, int prNumber, String reason) {
        String prUrl = "https://github.com/" + repoFullName + "/pull/" + prNumber;

        String message = """
            {
                "text": "❌ *AI Review Failed*\\n*PR:* <%s|%s #%d>\\n*Reason:* %s"
            }
            """.formatted(prUrl, repoFullName, prNumber, reason);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.postForEntity(
                webhookUrl,
                new HttpEntity<String>(message, headers),
                String.class
            );
        } catch (Exception e) {
            log.error("Failed to send Slack failure alert: {}", e.getMessage());
        }
    }

    private String buildSlackMessage(
            String repoFullName, int prNumber, String prUrl,
            int total, long critical, long warning, long info,
            long durationMs, int qualityScore, String scoreEmoji) {

        String statusEmoji = critical > 0 ? "🔴" : warning > 0 ? "⚠️" : "✅";

        return """
            {
                "blocks": [
                    {
                        "type": "header",
                        "text": {
                            "type": "plain_text",
                            "text": "%s AI Code Review Complete"
                        }
                    },
                    {
                        "type": "section",
                        "fields": [
                            {
                                "type": "mrkdwn",
                                "text": "*Repository:*\\n%s"
                            },
                            {
                                "type": "mrkdwn",
                                "text": "*Pull Request:*\\n<%s|PR #%d>"
                            }
                        ]
                    },
                    {
                        "type": "section",
                        "text": {
                            "type": "mrkdwn",
                            "text": "*Quality Score:* %s *%d/100*"
                        }
                    },
                    {
                        "type": "section",
                        "fields": [
                            {
                                "type": "mrkdwn",
                                "text": "*🔴 Critical:* %d"
                            },
                            {
                                "type": "mrkdwn",
                                "text": "*⚠️ Warning:* %d"
                            },
                            {
                                "type": "mrkdwn",
                                "text": "*💡 Info:* %d"
                            },
                            {
                                "type": "mrkdwn",
                                "text": "*📝 Total Issues:* %d"
                            }
                        ]
                    },
                    {
                        "type": "context",
                        "elements": [
                            {
                                "type": "mrkdwn",
                                "text": "⏱️ Review completed in %dms"
                            }
                        ]
                    }
                ]
            }
            """.formatted(
                statusEmoji,
                repoFullName,
                prUrl, prNumber,
                scoreEmoji, qualityScore,
                critical, warning, info, total,
                durationMs
            );
    }
}