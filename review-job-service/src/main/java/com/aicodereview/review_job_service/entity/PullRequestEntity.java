package com.aicodereview.review_job_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pull_requests")
@Data
public class PullRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "repo_full_name")
    private String repoFullName;

    @Column(name = "pr_number")
    private Integer prNumber;

    private String title;
    private String author;

    @Column(name = "commit_sha")
    private String commitSha;

    private String status; // pending / reviewing / completed / failed

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.openedAt = Instant.now();
        if (this.status == null) this.status = "pending";
    }
}