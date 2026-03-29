package com.aicodereview.review_job_service.service;

import com.aicodereview.review_job_service.model.ReviewComment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class QualityScoreService {

    // Deductions per issue severity
    private static final int CRITICAL_DEDUCTION = 20;
    private static final int WARNING_DEDUCTION  = 10;
    private static final int INFO_DEDUCTION     = 2;
    private static final int MAX_SCORE          = 100;
    private static final int MIN_SCORE          = 0;

    public int calculate(List<ReviewComment> comments) {
        if (comments.isEmpty()) {
            log.info("No issues found — perfect score: 100");
            return 100;
        }

        long critical = comments.stream()
            .filter(c -> "CRITICAL".equals(c.getSeverity())).count();
        long warning = comments.stream()
            .filter(c -> "WARNING".equals(c.getSeverity())).count();
        long info = comments.stream()
            .filter(c -> "INFO".equals(c.getSeverity())).count();

        int deduction = (int) (
            critical * CRITICAL_DEDUCTION +
            warning  * WARNING_DEDUCTION  +
            info     * INFO_DEDUCTION
        );

        int score = Math.max(MIN_SCORE, MAX_SCORE - deduction);

        log.info("Quality score: {}/100 (critical:{} warning:{} info:{})",
            score, critical, warning, info);

        return score;
    }

    public String getGrade(int score) {
        if (score >= 90) return "A ✅";
        if (score >= 75) return "B 👍";
        if (score >= 60) return "C ⚠️";
        if (score >= 40) return "D ❌";
        return "F 🔴";
    }

    public String getEmoji(int score) {
        if (score >= 90) return "✅";
        if (score >= 75) return "👍";
        if (score >= 60) return "⚠️";
        return "❌";
    }
}