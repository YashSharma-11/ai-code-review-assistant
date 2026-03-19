package com.aicodereview.webhook_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC = "pr.received";

    public void publishPrEvent(String repoName, String payload) {
        // Using repo name as key — ensures all PRs from same repo
        // go to same partition, preserving order per repo
        kafkaTemplate.send(TOPIC, repoName, payload)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish PR event to Kafka: {}", ex.getMessage());
                } else {
                    log.info("Published to Kafka topic '{}', offset: {}",
                        TOPIC,
                        result.getRecordMetadata().offset());
                }
            });
    }
}