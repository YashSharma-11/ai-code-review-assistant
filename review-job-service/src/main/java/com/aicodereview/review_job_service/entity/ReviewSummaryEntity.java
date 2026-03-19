package com.aicodereview.review_job_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "review_summaries")
@Data
public class ReviewSummaryEntity {

    @Id
    @Column(name = "pr_id")
    private UUID prId;

    @Column(name = "total_issues")
    private Integer totalIssues;

    @Column(name = "critical_count")
    private Integer criticalCount;

    @Column(name = "warning_count")
    private Integer warningCount;

    @Column(name = "info_count")
    private Integer infoCount;

    @Column(name = "review_duration_ms")
    private Integer reviewDurationMs;

    @Column(name = "ai_model")
    private String aiModel;
}