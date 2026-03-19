package com.aicodereview.review_job_service.github;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class GitHubApiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${github.token}")
    private String githubToken;

    // Fetches the raw unified diff for a PR
    public String getPullRequestDiff(String repoFullName, int prNumber) {
        String url = "https://api.github.com/repos/" + repoFullName
                + "/pulls/" + prNumber;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubToken);
        // This Accept header tells GitHub to return the diff format
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