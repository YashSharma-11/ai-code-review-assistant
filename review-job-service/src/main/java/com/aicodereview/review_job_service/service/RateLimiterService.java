package com.aicodereview.review_job_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {

    private final RedisTemplate<String, String> redisTemplate;

    // Groq free tier — 30 requests per minute
    private static final int MAX_REQUESTS_PER_MINUTE = 25; // conservative

    // GitHub API — 5000 requests per hour
    private static final int MAX_GITHUB_REQUESTS_PER_HOUR = 100; // conservative

    /**
     * Check and record a Groq API request.
     * Returns true if request is allowed, false if rate limit reached.
     */
    public boolean tryGroqRequest() {
        return tryRequest("rate:groq:", Duration.ofMinutes(1), MAX_REQUESTS_PER_MINUTE);
    }

    /**
     * Check and record a GitHub API request.
     * Returns true if request is allowed, false if rate limit reached.
     */
    public boolean tryGitHubRequest() {
        return tryRequest("rate:github:", Duration.ofHours(1), MAX_GITHUB_REQUESTS_PER_HOUR);
    }

    /**
     * Wait until Groq rate limit has capacity.
     * Blocks with exponential backoff until allowed.
     */
    public void waitForGroqCapacity() throws InterruptedException {
        int attempt = 0;
        while (!tryGroqRequest()) {
            long waitMs = Math.min(1000L * (attempt + 1), 10000L);
            log.warn("Groq rate limit reached — waiting {}ms (attempt {})",
                waitMs, attempt + 1);
            Thread.sleep(waitMs);
            attempt++;

            if (attempt > 10) {
                throw new RuntimeException("Groq rate limit exhausted after 10 attempts");
            }
        }
    }

    /**
     * Core rate limiting logic using Redis INCR + TTL.
     * Atomic operation — safe across multiple service instances.
     */
    private boolean tryRequest(String keyPrefix, Duration window, int maxRequests) {
        // Key includes current time window
        // e.g. "rate:groq:2024-01-15T10:30" for per-minute tracking
        String timeWindow = Instant.now()
            .truncatedTo(window.toMinutes() > 0 ? ChronoUnit.MINUTES : ChronoUnit.HOURS)
            .toString()
            .substring(0, window.toMinutes() > 0 ? 16 : 13); // "2024-01-15T10:30" or "2024-01-15T10"

        String key = keyPrefix + timeWindow;

        // Atomic increment
        Long count = redisTemplate.opsForValue().increment(key);

        // Set TTL on first request in this window
        if (count != null && count == 1) {
            redisTemplate.expire(key, window);
        }

        boolean allowed = count != null && count <= maxRequests;

        if (!allowed) {
            log.warn("Rate limit hit for {} — count: {}/{}", keyPrefix, count, maxRequests);
        } else {
            log.debug("Rate limit OK for {} — count: {}/{}", keyPrefix, count, maxRequests);
        }

        return allowed;
    }

    /**
     * Get current request count for monitoring.
     */
    public long getCurrentGroqRequestCount() {
        String timeWindow = Instant.now()
            .truncatedTo(ChronoUnit.MINUTES)
            .toString()
            .substring(0, 16);
        String key = "rate:groq:" + timeWindow;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0;
    }
}