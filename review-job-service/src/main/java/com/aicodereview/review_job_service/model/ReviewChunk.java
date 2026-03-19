package com.aicodereview.review_job_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class ReviewChunk {
    private String content;
    private List<String> files;
}