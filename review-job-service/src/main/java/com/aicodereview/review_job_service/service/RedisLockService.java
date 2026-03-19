package com.aicodereview.review_job_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLockService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    // Returns true if lock was acquired (safe to proceed)
    public boolean tryLock(String prId) {
        String key = "review:lock:" + prId;

        // SET NX — only sets if key doesn't exist
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(key, "locked", LOCK_TTL);

        boolean locked = Boolean.TRUE.equals(acquired);

        if (!locked) {
            log.info("PR {} is already being reviewed — skipping duplicate", prId);
        } else {
            log.info("Lock acquired for PR {}", prId);
        }

        return locked;
    }

    public void releaseLock(String prId) {
        String key = "review:lock:" + prId;
        redisTemplate.delete(key);
        log.info("Lock released for PR {}", prId);
    }
}