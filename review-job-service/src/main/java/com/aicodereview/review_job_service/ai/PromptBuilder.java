package com.aicodereview.review_job_service.ai;

import com.aicodereview.review_job_service.model.ReviewChunk;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String buildSystemPrompt(LanguageDetector.Language language) {
        String languageSpecific = switch (language) {
            case JAVA -> """
                You are a senior Java engineer conducting a code review.
                Focus on: null pointer exceptions, N+1 queries, resource leaks,
                missing error handling, SQL injection, SOLID violations,
                improper use of Optional, unchecked exceptions, thread safety issues.
                """;
            case PYTHON -> """
                You are a senior Python engineer conducting a code review.
                Focus on: None checks, mutable default arguments, improper exception handling,
                missing type hints, PEP8 violations, memory leaks, SQL injection,
                improper use of list comprehensions, global state issues.
                """;
            case JAVASCRIPT -> """
                You are a senior JavaScript engineer conducting a code review.
                Focus on: null/undefined checks, async/await issues, callback hell,
                XSS vulnerabilities, memory leaks, improper promise handling,
                var vs let/const, missing error handling in async code.
                """;
            case TYPESCRIPT -> """
                You are a senior TypeScript engineer conducting a code review.
                Focus on: type safety issues, any type usage, null/undefined handling,
                improper generics usage, async/await issues, interface violations,
                missing return types, XSS vulnerabilities.
                """;
            case GO -> """
                You are a senior Go engineer conducting a code review.
                Focus on: error handling (unchecked errors), goroutine leaks,
                race conditions, improper use of defer, nil pointer dereferences,
                missing context cancellation, improper channel usage.
                """;
            default -> """
                You are a senior software engineer conducting a code review.
                Focus on: bugs, security vulnerabilities, performance issues,
                missing error handling, and code quality issues.
                """;
        };

        return languageSpecific + """

            Review ONLY the changed lines shown in the diff.

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
    }

    public String buildUserPrompt(ReviewChunk chunk) {
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