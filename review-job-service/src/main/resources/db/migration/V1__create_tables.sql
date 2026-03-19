-- Pull requests that were reviewed
CREATE TABLE pull_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repo_full_name VARCHAR(200) NOT NULL,
    pr_number INTEGER NOT NULL,
    title VARCHAR(500),
    author VARCHAR(100),
    commit_sha VARCHAR(40),
    status VARCHAR(20) DEFAULT 'pending',
    opened_at TIMESTAMP DEFAULT now(),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now(),
    UNIQUE(repo_full_name, pr_number)
);

-- AI generated comments per review
CREATE TABLE review_comments (
    id BIGSERIAL PRIMARY KEY,
    pr_id UUID NOT NULL REFERENCES pull_requests(id),
    file_path VARCHAR(500) NOT NULL,
    line_number INTEGER NOT NULL,
    severity VARCHAR(20) NOT NULL,
    category VARCHAR(30) NOT NULL,
    comment TEXT NOT NULL,
    suggestion TEXT,
    created_at TIMESTAMP DEFAULT now()
);

-- Summary stats per review
CREATE TABLE review_summaries (
    pr_id UUID PRIMARY KEY REFERENCES pull_requests(id),
    total_issues INTEGER DEFAULT 0,
    critical_count INTEGER DEFAULT 0,
    warning_count INTEGER DEFAULT 0,
    info_count INTEGER DEFAULT 0,
    review_duration_ms INTEGER,
    ai_model VARCHAR(50) DEFAULT 'groq-llama3'
);

-- Indexes for fast dashboard queries
CREATE INDEX idx_prs_repo ON pull_requests(repo_full_name);
CREATE INDEX idx_prs_status ON pull_requests(status);
CREATE INDEX idx_comments_pr ON review_comments(pr_id);
CREATE INDEX idx_comments_severity ON review_comments(severity);