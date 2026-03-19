package com.aicodereview.review_job_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String DEAD_LETTER_TOPIC = "review.failed";

    public void sendToDeadLetter(String payload, String reason) {
        String message = """
            {
                "reason": "%s",
                "timestamp": "%s",
                "payload": %s
            }
            """.formatted(reason, java.time.Instant.now(), payload);

        kafkaTemplate.send(DEAD_LETTER_TOPIC, message)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send to dead letter queue: {}", ex.getMessage());
                } else {
                    log.warn("Sent failed review to dead letter queue");
                }
            });
    }
}