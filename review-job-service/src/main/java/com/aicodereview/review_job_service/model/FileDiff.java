package com.aicodereview.review_job_service.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class FileDiff {
    private String fileName;
    private List<DiffLine> lines = new ArrayList<>();

    public FileDiff(String fileName) {
        this.fileName = fileName;
    }

    public void addLine(DiffLine line) {
        this.lines.add(line);
    }

    public String toReviewableText() {
        StringBuilder sb = new StringBuilder();
        sb.append("File: ").append(fileName).append("\n");
        for (DiffLine line : lines) {
            sb.append("+").append(line.getLineNumber())
              .append(": ").append(line.getContent()).append("\n");
        }
        return sb.toString();
    }
}