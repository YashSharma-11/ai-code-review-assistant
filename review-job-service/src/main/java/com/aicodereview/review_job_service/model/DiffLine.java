package com.aicodereview.review_job_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DiffLine {
    private int lineNumber;
    private String content;
}