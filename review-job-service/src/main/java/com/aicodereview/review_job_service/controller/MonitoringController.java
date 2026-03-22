package com.aicodereview.review_job_service.controller;

import com.aicodereview.review_job_service.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitoringController {

    private final RateLimiterService rateLimiterService;

    @GetMapping("/rate-limits")
    public ResponseEntity<Map<String, Object>> getRateLimits() {
        return ResponseEntity.ok(Map.of(
            "groq_requests_this_minute", rateLimiterService.getCurrentGroqRequestCount(),
            "groq_limit_per_minute", 25,
            "status", "ok"
        ));
    }
}