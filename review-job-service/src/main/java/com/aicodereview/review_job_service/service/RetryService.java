package com.aicodereview.review_job_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
@Slf4j
public class RetryService {

    private static final int MAX_ATTEMPTS = 3;

    // Retries with exponential backoff: 1s, 2s, 4s
    public <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        int attempt = 0;

        while (attempt < MAX_ATTEMPTS) {
            try {
                return operation.get();
            } catch (Exception e) {
                attempt++;
                if (attempt >= MAX_ATTEMPTS) {
                    log.error("{} failed after {} attempts: {}",
                        operationName, MAX_ATTEMPTS, e.getMessage());
                    throw new RuntimeException(operationName + " failed after retries", e);
                }

                long waitMs = (long) Math.pow(2, attempt) * 1000;
                log.warn("{} failed (attempt {}/{}), retrying in {}ms — {}",
                    operationName, attempt, MAX_ATTEMPTS, waitMs, e.getMessage());

                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        throw new RuntimeException(operationName + " failed");
    }
}