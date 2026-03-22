package com.aicodereview.review_job_service.github;

import com.aicodereview.review_job_service.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class GitHubApiClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final RateLimiterService rateLimiterService;

    @Value("${github.token}")
    private String githubToken;

    public String getPullRequestDiff(String repoFullName, int prNumber) {

        if (!rateLimiterService.tryGitHubRequest()) {
            log.warn("GitHub rate limit reached — waiting before retry");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        String url = "https://api.github.com/repos/" + repoFullName
                + "/pulls/" + prNumber;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubToken);
        headers.set("Accept", "application/vnd.github.v3.diff");

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
            );
            log.info("Fetched diff for PR #{} in {}", prNumber, repoFullName);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch PR diff: {}", e.getMessage());
            return "";
        }
    }
}