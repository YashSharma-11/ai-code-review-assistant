package com.aicodereview.review_job_service.repository;

import com.aicodereview.review_job_service.entity.PullRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PullRequestRepository
        extends JpaRepository<PullRequestEntity, UUID> {

    Optional<PullRequestEntity> findByRepoFullNameAndPrNumber(
        String repoFullName, Integer prNumber
    );
}