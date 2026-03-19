package com.aicodereview.review_job_service.repository;

import com.aicodereview.review_job_service.entity.ReviewCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewCommentRepository
        extends JpaRepository<ReviewCommentEntity, Long> {

    List<ReviewCommentEntity> findByPrId(UUID prId);
}