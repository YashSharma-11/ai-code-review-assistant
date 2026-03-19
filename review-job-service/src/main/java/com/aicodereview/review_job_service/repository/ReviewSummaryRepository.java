package com.aicodereview.review_job_service.repository;

import com.aicodereview.review_job_service.entity.ReviewSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewSummaryRepository
        extends JpaRepository<ReviewSummaryEntity, UUID> {
}