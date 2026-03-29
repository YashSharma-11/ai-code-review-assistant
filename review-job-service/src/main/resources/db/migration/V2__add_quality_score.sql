-- Add quality score to review_summaries
ALTER TABLE review_summaries ADD COLUMN quality_score INTEGER DEFAULT 100;

-- Add quality score to pull_requests for easy querying
ALTER TABLE pull_requests ADD COLUMN quality_score INTEGER;