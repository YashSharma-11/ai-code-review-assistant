package com.aicodereview.review_job_service.github;

import com.aicodereview.review_job_service.model.DiffLine;
import com.aicodereview.review_job_service.model.FileDiff;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class DiffParser {

    // Matches hunk header: @@ -23,7 +23,8 @@
    private static final Pattern HUNK_PATTERN =
        Pattern.compile("^@@ -\\d+(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@");

    public List<FileDiff> parse(String rawDiff) {
        List<FileDiff> result = new ArrayList<>();

        if (rawDiff == null || rawDiff.isBlank()) {
            return result;
        }

        FileDiff current = null;
        int newLineNumber = 0;

        for (String line : rawDiff.split("\n")) {

            if (line.startsWith("+++ b/")) {
                // New file section — extract filename
                String fileName = line.substring(6);
                current = new FileDiff(fileName);
                result.add(current);

            } else if (line.startsWith("@@")) {
                // Hunk header — extract new file start line number
                Matcher matcher = HUNK_PATTERN.matcher(line);
                if (matcher.find()) {
                    newLineNumber = Integer.parseInt(matcher.group(1));
                }

            } else if (line.startsWith("+") && !line.startsWith("+++")) {
                // Added line — this is what we review
                if (current != null) {
                    current.addLine(new DiffLine(newLineNumber, line.substring(1)));
                }
                newLineNumber++;

            } else if (line.startsWith("-")) {
                // Removed line — don't increment new file line number

            } else {
                // Context line — increment line number
                newLineNumber++;
            }
        }

        log.info("Parsed {} files from diff", result.size());
        return result;
    }
}