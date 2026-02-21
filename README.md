# Job Scraper Microservices Architecture

## Overview

Event-driven microservices architecture for automated job scraping and AI-powered ranking.
<img width="657" height="1359" alt="overview" src="https://github.com/user-attachments/assets/528b47d1-639d-4f00-a249-61090ad4c07a" />

### Request Flow

1. User creates task → Gateway validates JWT → Task Service
2. Task Service saves to DB, publishes scrape.requested
3. Scraper consumes → scrapes Indeed via Scrapfly
4. Scraper publishes scrape.completed with jobs
5. Ranker consumes → scores jobs via Gemini AI → saves to Jobs DB
6. Ranker publishes rank.completed
7. Task Service consumes → updates task status to COMPLETED
8. User fetches ranked jobs → Gateway → Ranker Service
---

## Demo
[![Watch the demo](https://img.youtube.com/vi/Y4VrE4khcS4/0.jpg)](https://youtu.be/Y4VrE4khcS4)

---
## Services

### Task Service (Port 8080)
**Purpose:** Orchestrator - manages scraping jobs, scheduling, status tracking

| Component | Details |
|-----------|---------|
| Database | PostgreSQL (own instance) |
| Consumes | `scrape.completed`, `scrape.failed`, `rank.completed`, `rank.failed` |
| Publishes | `scrape.requested` |

**Key Features:**
- CRUD for tasks (job search configurations)
- Interval-based scheduling (every X hours)
- Status tracking: `SCHEDULED` → `SCRAPING` → `RANKING` → `COMPLETED`
- Stuck task detection

**REST Endpoints:**
```
POST   /api/v1/tasks
GET    /api/v1/tasks
GET    /api/v1/tasks/{id}
PUT    /api/v1/tasks/{id}
DELETE /api/v1/tasks/{id}
POST   /api/v1/tasks/{id}/run
POST   /api/v1/tasks/{id}/pause
POST   /api/v1/tasks/{id}/resume
```

---

### Scraper Service (Port 8081)
**Purpose:** Stateless worker - scrapes Indeed.com via Scrapfly proxy

| Component | Details |
|-----------|---------|
| Database | None |
| Consumes | `scrape.requested` |
| Publishes | `scrape.completed`, `scrape.failed` |

**Key Features:**
- Indeed job search scraping
- Job detail page scraping (full descriptions)
- Retry logic with session rotation
- Login redirect detection

**External Dependencies:**
- Scrapfly API (proxy/anti-bot)

---

### Ranker Service (Port 8082)
**Purpose:** AI ranking + job storage + REST API for frontend

| Component | Details |
|-----------|---------|
| Database | PostgreSQL (own instance) |
| Consumes | `scrape.completed` |
| Publishes | `rank.completed`, `rank.failed` |

**Key Features:**
- Gemini AI ranking (1-10 score)
- Job storage with bucket support (NONE, APPLIED)
- REST API for frontend consumption

**REST Endpoints:**
```
GET    /api/v1/jobs
GET    /api/v1/jobs/{id}
GET    /api/v1/jobs/task/{taskId}
PATCH  /api/v1/jobs/{id}/bucket
DELETE /api/v1/jobs/{id}
DELETE /api/v1/jobs/task/{taskId}
DELETE /api/v1/jobs
GET    /api/v1/jobs/count
```

**External Dependencies:**
- Gemini API

---

## Infrastructure

| Component | Port | Purpose |
|-----------|------|---------|
| PostgreSQL (Task) | 5432 | Task storage |
| PostgreSQL (Ranker) | 5433 | Job storage |
| Kafka | 9092 (internal), 9094 (external) | Event bus |
| Kafka UI | 8090 | Monitoring |

---

## Kafka Topics

| Topic | Publisher | Consumer |
|-------|-----------|----------|
| `scrape.requested` | Task Service | Scraper Service |
| `scrape.completed` | Scraper Service | Task Service, Ranker Service |
| `scrape.failed` | Scraper Service | Task Service |
| `rank.completed` | Ranker Service | Task Service |
| `rank.failed` | Ranker Service | Task Service |

---

## Data Flow

```
1. User creates task → Task Service saves to DB
2. Task due → Task Service publishes scrape.requested
3. Scraper scrapes Indeed → publishes scrape.completed (with jobs + preferences)
4. Ranker ranks via Gemini → saves jobs → publishes rank.completed
5. Task Service updates status to COMPLETED
6. Frontend fetches ranked jobs from Ranker Service REST API
```

---

## Environment Variables

### Task Service
```
DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD
KAFKA_BOOTSTRAP_SERVERS
```

### Scraper Service
```
KAFKA_BOOTSTRAP_SERVERS
SCRAPFLY_API_KEY
```

### Ranker Service
```
DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD
KAFKA_BOOTSTRAP_SERVERS
GEMINI_API_KEY
```

---

## Project Checklist

- [x] Task Service - core CRUD
- [x] Task Service - scheduling (interval-based)
- [x] Task Service - Kafka producer/consumer
- [x] Scraper Service - Indeed search scraping
- [x] Scraper Service - job detail scraping
- [x] Scraper Service - Kafka integration
- [x] Scraper Service - retry logic
- [x] Ranker Service - Gemini integration
- [x] Auth Service
- [x] API Gateway
- [x] Frontend
