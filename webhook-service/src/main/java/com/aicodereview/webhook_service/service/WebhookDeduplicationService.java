package com.aicodereview.webhook_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDeduplicationService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final Duration DEDUP_WINDOW = Duration.ofHours(24);

    // Returns true if this is a NEW event (not a duplicate)
    public boolean isNewEvent(String deliveryId) {
        String key = "webhook:seen:" + deliveryId;

        // SET key value NX EX — atomic: set only if not exists, with TTL
        Boolean wasAbsent = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", DEDUP_WINDOW);

        boolean isNew = Boolean.TRUE.equals(wasAbsent);

        if (!isNew) {
            log.info("Duplicate webhook detected, ignoring: {}", deliveryId);
        }

        return isNew;
    }
}