package com.aicodereview.review_job_service.ai;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LanguageDetector {

    public enum Language {
        JAVA, PYTHON, JAVASCRIPT, TYPESCRIPT, GO, UNKNOWN
    }

    // Detect language from file extension
    public Language detect(List<String> fileNames) {
        for (String fileName : fileNames) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".java"))       return Language.JAVA;
            if (lower.endsWith(".py"))         return Language.PYTHON;
            if (lower.endsWith(".js"))         return Language.JAVASCRIPT;
            if (lower.endsWith(".ts"))         return Language.TYPESCRIPT;
            if (lower.endsWith(".go"))         return Language.GO;
        }
        return Language.UNKNOWN;
    }
}