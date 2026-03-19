# 🤖 AI Code Review Assistant

An AI-powered GitHub Pull Request review system that automatically 
analyses code changes using Groq LLM and posts inline review comments 
— acting as a senior engineer on every PR.

## 🏗️ Architecture
```
Developer opens PR
        ↓
GitHub Webhook → Webhook Service (port 8080)
        ↓ HMAC validation + Redis dedup
        ↓
Kafka Topic: pr.received
        ↓
Review Job Service (port 8081)
        ↓ Fetch diff → Parse → Chunk → Groq AI
        ↓
GitHub PR Comments + PostgreSQL + Slack
```

## ⚙️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Message Queue | Apache Kafka |
| Cache / Lock | Redis |
| Database | PostgreSQL + Flyway |
| AI Model | Groq llama-3.3-70b |
| Notifications | Slack Webhooks |
| Container | Docker + Docker Compose |
| CI/CD | GitHub Actions |

## 🔑 Key Engineering Decisions

**Why Kafka?**
GitHub webhooks must respond in <10 seconds but AI review 
takes 2-3 seconds. Kafka decouples ingestion from processing — 
webhook responds in <5ms, review happens asynchronously.

**Why Redis SET NX for deduplication?**
GitHub can redeliver webhooks. Redis atomic SET NX stores 
delivery ID with 24h TTL — duplicate deliveries are silently 
ignored without any race condition.

**Why distributed locking?**
3 concurrent Kafka consumer threads could review the same PR 
simultaneously. Redis lock with PR ID as key prevents duplicate 
reviews and duplicate GitHub comments.

**Why manual Kafka acknowledgment?**
Default auto-ack would acknowledge even on processing failure. 
Manual ack guarantees at-least-once processing — no PR is 
silently skipped.

## 🚀 Quick Start

### Prerequisites
- Docker Desktop
- ngrok account (free)
- GitHub App
- Groq API key (free)
- Slack webhook URL

### Run the full stack
```bash
# Clone the repo
git clone https://github.com/YashSharma-11/ai-code-review.git
cd ai-code-review

# Set environment variables
cp .env.example .env
# Edit .env with your actual keys

# Start infrastructure
docker-compose up -d zookeeper kafka redis postgres

# Start services
docker-compose up webhook-service review-job-service
```

### Start ngrok
```bash
ngrok http 8080
```

Update your GitHub App webhook URL with the ngrok URL.

## 📊 Database Schema
```sql
pull_requests      — Every PR reviewed (status, timing)
review_comments    — Every AI comment (file, line, severity)
review_summaries   — Stats per review (counts, duration)
```

## 🔁 Flow Details

1. Developer opens PR on GitHub
2. GitHub sends POST to `/api/webhooks/github`
3. HMAC-SHA256 signature validated
4. Redis deduplication check (SET NX)
5. Event published to Kafka `pr.received`
6. Consumer acquires Redis distributed lock
7. PR diff fetched from GitHub API
8. Diff parsed — changed lines extracted with line numbers
9. Code chunked to fit LLM context window
10. Groq AI reviews each chunk — returns JSON comments
11. Comments posted to GitHub PR
12. Results saved to PostgreSQL
13. Slack notification sent to team channel
14. Kafka message acknowledged
15. Redis lock released

## 📈 Performance

| Metric | Value |
|---|---|
| Webhook response time | < 5ms |
| End-to-end review time | 2-3 seconds |
| PRs reviewed in testing | 24+ |
| Kafka topics | 2 (pr.received, review.failed) |
| Concurrent workers | 3 |