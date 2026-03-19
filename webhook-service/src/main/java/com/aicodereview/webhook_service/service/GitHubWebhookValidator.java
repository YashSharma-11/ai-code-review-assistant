package com.aicodereview.webhook_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
@Slf4j
public class GitHubWebhookValidator {

    @Value("${github.webhook.secret}")
    private String webhookSecret;

    public boolean isValidSignature(String payload, String signatureHeader) {

        // GitHub sends: X-Hub-Signature-256: sha256=<hex>
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            log.warn("Missing or invalid signature header");
            return false;
        }

        String receivedSig = signatureHeader.substring(7);

        try {
            // Compute expected HMAC-SHA256
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            ));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSig = HexFormat.of().formatHex(hash);

            // Constant-time comparison — prevents timing attacks
            boolean valid = MessageDigest.isEqual(
                expectedSig.getBytes(),
                receivedSig.getBytes()
            );

            if (!valid) {
                log.warn("Signature mismatch! Possible fake webhook.");
            }

            return valid;

        } catch (Exception e) {
            log.error("Signature validation error", e);
            return false;
        }
    }
}