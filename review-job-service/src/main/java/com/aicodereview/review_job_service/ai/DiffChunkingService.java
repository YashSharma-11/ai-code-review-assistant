package com.aicodereview.review_job_service.ai;

import com.aicodereview.review_job_service.model.FileDiff;
import com.aicodereview.review_job_service.model.ReviewChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DiffChunkingService {

    private static final int MAX_CHARS = 6000 * 4; // ~6000 tokens

    public List<ReviewChunk> chunk(List<FileDiff> diffs) {
        List<ReviewChunk> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        List<String> filesInChunk = new ArrayList<>();

        for (FileDiff diff : diffs) {
            String diffText = diff.toReviewableText();

            if (current.length() + diffText.length() > MAX_CHARS) {
                if (current.length() > 0) {
                    chunks.add(new ReviewChunk(current.toString(), filesInChunk));
                    current = new StringBuilder();
                    filesInChunk = new ArrayList<>();
                }
            }

            if (diffText.length() > MAX_CHARS) {
                diffText = diffText.substring(0, MAX_CHARS);
                log.warn("File {} truncated — too large", diff.getFileName());
            }

            current.append(diffText);
            filesInChunk.add(diff.getFileName());
        }

        if (current.length() > 0) {
            chunks.add(new ReviewChunk(current.toString(), filesInChunk));
        }

        log.info("Split {} files into {} chunks", diffs.size(), chunks.size());
        return chunks;
    }
}