package com.aicodereview.review_job_service.ai;

import com.aicodereview.review_job_service.model.ReviewChunk;
import com.aicodereview.review_job_service.model.ReviewComment;
import com.aicodereview.review_job_service.service.RateLimiterService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiReviewService {

    @Value("${groq.api-key}")
    private String apiKey;

    private final RateLimiterService rateLimiterService;
    private ChatLanguageModel model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        this.model = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl("https://api.groq.com/openai/v1")
            .modelName("llama-3.3-70b-versatile")
            .temperature(0.1)
            .maxTokens(2000)
            .timeout(Duration.ofSeconds(60))
            .build();
        log.info("✅ Groq llama3-70b model initialized");
    }

    private static final String SYSTEM_PROMPT = """
        You are a senior Java engineer conducting a code review.
        Review ONLY the changed lines shown in the diff.
        Focus on: bugs, security vulnerabilities, performance issues,
        null pointer risks, SQL injection, N+1 queries, resource leaks,
        missing error handling, and violations of SOLID principles.

        Return a JSON array. Each element must have:
        - file: exact filename from the diff
        - line: the line NUMBER in the new file
        - severity: CRITICAL | WARNING | INFO
        - category: BUG | SECURITY | PERFORMANCE | STYLE | DESIGN
        - comment: specific actionable feedback (max 200 chars)
        - suggestion: the corrected code snippet (optional, can be null)

        If the code looks good, return an empty array [].
        Return ONLY valid JSON. No markdown, no explanation, no backticks.
        """;

    public List<ReviewComment> reviewChunk(ReviewChunk chunk) {
        try {
            // Wait for Groq capacity before calling API
            rateLimiterService.waitForGroqCapacity();

            log.info("Sending chunk to Groq — files: {}", chunk.getFiles());

            String response = model.generate(
                dev.langchain4j.data.message.SystemMessage.from(SYSTEM_PROMPT),
                dev.langchain4j.data.message.UserMessage.from(buildUserPrompt(chunk))
            ).content().text();

            log.info("Groq responded: {}", response);

            String cleaned = response
                .replace("```json", "")
                .replace("```", "")
                .trim();

            List<ReviewComment> comments = objectMapper.readValue(
                cleaned,
                new TypeReference<List<ReviewComment>>() {}
            );

            log.info("Groq found {} issues", comments.size());
            return comments;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Rate limiter interrupted: {}", e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Failed to get AI review: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildUserPrompt(ReviewChunk chunk) {
        return """
            Review the following code changes:

            Files: %s

            Diff:
            %s

            Remember: return ONLY a JSON array, nothing else.
            """.formatted(
                String.join(", ", chunk.getFiles()),
                chunk.getContent()
            );
    }
}