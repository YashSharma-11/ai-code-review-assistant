package com.aicodereview.review_job_service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewComment {
    private String file;
    private int line;
    private String severity;   // CRITICAL / WARNING / INFO
    private String category;   // BUG / SECURITY / PERFORMANCE / STYLE
    private String comment;
    private String suggestion; // optional
}