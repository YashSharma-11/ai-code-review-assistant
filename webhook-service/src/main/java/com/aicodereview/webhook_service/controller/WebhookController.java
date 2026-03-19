package com.aicodereview.webhook_service.controller;

import com.aicodereview.webhook_service.service.GitHubWebhookValidator;
import com.aicodereview.webhook_service.service.WebhookDeduplicationService;
import com.aicodereview.webhook_service.service.WebhookEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final GitHubWebhookValidator validator;
    private final WebhookDeduplicationService deduplicationService;
    private final WebhookEventProducer eventProducer;

    @PostMapping("/api/webhooks/github")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "unknown") String eventType,
            @RequestHeader(value = "X-GitHub-Delivery", defaultValue = "unknown") String deliveryId,
            @RequestHeader(value = "X-Hub-Signature-256", defaultValue = "") String signature,
            @RequestHeader(value = "X-GitHub-Hook-Installation-Target-ID", defaultValue = "unknown") String repoId,
            @RequestBody String payload) {

        log.info("Webhook received — Event: {}, DeliveryID: {}", eventType, deliveryId);

        // STEP 1 — Validate HMAC signature
        if (!validator.isValidSignature(payload, signature)) {
            log.warn("Invalid signature — rejecting webhook");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("✅ Signature valid");

        // STEP 2 — Deduplicate
        if (!deduplicationService.isNewEvent(deliveryId)) {
            log.info("Duplicate webhook ignored: {}", deliveryId);
            return ResponseEntity.ok().build();
        }
        log.info("✅ New event, not a duplicate");

        // STEP 3 — Only process pull_request events
        if (!eventType.equals("pull_request")) {
            log.info("Ignoring non-PR event: {}", eventType);
            return ResponseEntity.ok().build();
        }

        // STEP 4 — Publish to Kafka
        eventProducer.publishPrEvent(repoId, payload);
        log.info("✅ Published to Kafka");

        return ResponseEntity.ok().build();
    }
}