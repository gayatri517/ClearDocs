# ClearDocs — Document Approval Platform

![Java](https://img.shields.io/badge/Java-11-blue?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-brightgreen?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql)
![MongoDB](https://img.shields.io/badge/MongoDB-7-green?logo=mongodb)
![Redis](https://img.shields.io/badge/Redis-7-red?logo=redis)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

A production-ready **document approval platform** built with Java 11 and Spring Boot. Documents move through a **CQRS-influenced state machine** (Draft → Submitted → Under Review → Pending Approval → Approved/Rejected), with JWT + OAuth RBAC enforced by Spring Security, full audit logging via Spring AOP, and a multi-database persistence layer (PostgreSQL for relational data, MongoDB for metadata & audit logs, Redis for caching).

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────┐
│                        Client                             │
│          (Swagger UI / REST API / Frontend)               │
└──────────────────────┬───────────────────────────────────┘
                       │ HTTPS / JWT Bearer
┌──────────────────────▼───────────────────────────────────┐
│                Spring Boot Application                    │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────┐  │
│  │  Controllers │  │  AOP Audit   │  │ Spring Security│  │
│  │  (REST API) │  │  Aspects     │  │  + JWT Filter  │  │
│  └──────┬──────┘  └──────┬───────┘  └────────────────┘  │
│         │                │                               │
│  ┌──────▼──────┐  ┌──────▼───────┐                      │
│  │  Services   │  │ Audit Logger │                      │
│  │ (CQRS SM)  │  │  (MongoDB)   │                      │
│  └──────┬──────┘  └──────────────┘                      │
│         │                                                │
│  ┌──────▼──────────────────────────────────────────┐    │
│  │               Repository Layer                  │    │
│  │  JPA/Hibernate ──► PostgreSQL (normalized)      │    │
│  │  Spring Data MongoDB ──► MongoDB (metadata)     │    │
│  │  Spring Cache + Redis ──► Redis (caching)       │    │
│  └──────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────┘
```

## State Machine

```
DRAFT ──SUBMIT──► SUBMITTED ──START_REVIEW──► UNDER_REVIEW
  ▲                                                │
  │                            COMPLETE_REVIEW ◄───┘
  │                                    │
  │                                    ▼
  │                           PENDING_APPROVAL
  │                           /       |      \
  │                    APPROVE    REJECT  REQUEST_REVISION
  │                       │          │           │
  │                   APPROVED   REJECTED ◄───────┘
  │                       │          │
  │                   ARCHIVE    DRAFT/WITHDRAW
  │                       │
  └───────────────────ARCHIVED
                   (terminal)
          WITHDRAWN (terminal from most states)
```

## Tech Stack

| Layer              | Technology                              |
|--------------------|-----------------------------------------|
| Runtime            | Java 11 (Temurin)                       |
| Framework          | Spring Boot 2.7                         |
| Security           | Spring Security + JWT (JJWT 0.11)       |
| OAuth2             | Spring OAuth2 Resource Server           |
| AOP / Audit        | Spring AOP (`@Audited` annotation)      |
| ORM                | Hibernate 5 / Spring Data JPA           |
| Primary DB         | PostgreSQL 15 (Flyway migrations)       |
| Document Store     | MongoDB 7 (metadata & audit logs)       |
| Cache              | Redis 7 (Spring Cache abstraction)      |
| Validation         | Jakarta Validation (Bean Validation 2)  |
| API Docs           | SpringDoc OpenAPI 3 / Swagger UI        |
| Testing            | JUnit 5, Mockito, Testcontainers        |
| Coverage           | JaCoCo                                  |
| Containerisation   | Docker + Docker Compose                 |
| CI/CD              | GitHub Actions                          |

---

## Roles & Permissions

| Role        | Capabilities                                              |
|-------------|-----------------------------------------------------------|
| `ROLE_USER` | Create, update, submit, withdraw own documents            |
| `ROLE_REVIEWER` | Start review, request revision, reject documents      |
| `ROLE_APPROVER` | All reviewer actions + approve documents              |
| `ROLE_ADMIN`    | Full access to all endpoints                          |
| `ROLE_AUDITOR`  | Read-only access to audit logs                        |

---

## Quick Start — Docker Compose

### Prerequisites
- Docker Engine ≥ 24
- Docker Compose ≥ 2.20

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/cleardocs.git
cd cleardocs
```

### 2. Start all services

```bash
docker compose up -d
```

This starts:
- **PostgreSQL** on `localhost:5432`
- **MongoDB** on `localhost:27017`
- **Redis** on `localhost:6379`
- **ClearDocs API** on `localhost:8080`

Flyway migrations run automatically on startup and seed the database with default users.

### 3. Verify the app is running

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

### 4. Open Swagger UI

```
http://localhost:8080/api/v1/swagger-ui.html
```

---

## Default Users (seeded by Flyway)

| Username    | Password      | Roles             |
|-------------|---------------|-------------------|
| `admin`     | `Admin@1234`  | ADMIN, AUDITOR    |
| `reviewer1` | `Admin@1234`  | REVIEWER          |
| `approver1` | `Admin@1234`  | APPROVER          |

---

## API Reference

### Authentication

```bash
# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"Admin@1234"}'

# Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "email": "newuser@example.com",
    "password": "Secure@1234",
    "fullName": "New User",
    "roles": ["user"]
  }'
```

### Documents

```bash
# Create a document (multipart/form-data)
curl -X POST http://localhost:8080/api/v1/documents \
  -H "Authorization: Bearer <TOKEN>" \
  -F 'data={"title":"Q4 Budget","documentType":"FINANCIAL","priority":"HIGH","reviewerIds":[2]};type=application/json' \
  -F 'file=@budget.pdf'

# List documents
curl http://localhost:8080/api/v1/documents \
  -H "Authorization: Bearer <TOKEN>"

# Get a single document
curl http://localhost:8080/api/v1/documents/1 \
  -H "Authorization: Bearer <TOKEN>"

# Submit for review
curl -X POST http://localhost:8080/api/v1/documents/1/submit \
  -H "Authorization: Bearer <TOKEN>"
```

### Approval Workflow

```bash
# Start review (REVIEWER/APPROVER/ADMIN)
curl -X POST http://localhost:8080/api/v1/approvals/documents/1/start-review \
  -H "Authorization: Bearer <REVIEWER_TOKEN>"

# Approve (APPROVER/ADMIN)
curl -X POST http://localhost:8080/api/v1/approvals/documents/1/action \
  -H "Authorization: Bearer <APPROVER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"action":"APPROVE","comments":"Looks good!"}'

# Reject
curl -X POST http://localhost:8080/api/v1/approvals/documents/1/action \
  -H "Authorization: Bearer <APPROVER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"action":"REJECT","rejectionReason":"Missing signatures"}'

# Request revision
curl -X POST http://localhost:8080/api/v1/approvals/documents/1/action \
  -H "Authorization: Bearer <REVIEWER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"action":"REQUEST_REVISION","comments":"Please update section 3"}'

# Get my pending reviews
curl http://localhost:8080/api/v1/approvals/pending \
  -H "Authorization: Bearer <REVIEWER_TOKEN>"
```

### Audit Logs (ADMIN/AUDITOR only)

```bash
# All audit logs
curl http://localhost:8080/api/v1/audit \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# Document audit trail
curl http://localhost:8080/api/v1/audit/documents/1/trail \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# By time range
curl "http://localhost:8080/api/v1/audit/range?from=2024-01-01T00:00:00Z&to=2024-12-31T23:59:59Z" \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

---

## Running Tests

### Unit tests only

```bash
mvn test -Dtest="*Test" -DfailIfNoTests=false
```

### Integration tests (requires Docker for Testcontainers)

```bash
mvn verify -Dtest="*IntegrationTest" -DfailIfNoTests=false
```

### All tests + coverage report

```bash
mvn verify
open target/site/jacoco/index.html
```

---

## Local Development (without Docker)

### Prerequisites
- JDK 11
- Maven 3.8+
- PostgreSQL 15 running locally
- MongoDB 7 running locally
- Redis 7 running locally

### 1. Create the database

```sql
CREATE DATABASE cleardocs;
CREATE USER cleardocs WITH PASSWORD 'cleardocs';
GRANT ALL PRIVILEGES ON DATABASE cleardocs TO cleardocs;
```

### 2. Configure environment variables

```bash
export POSTGRES_URL=jdbc:postgresql://localhost:5432/cleardocs
export POSTGRES_USER=cleardocs
export POSTGRES_PASSWORD=cleardocs
export MONGO_URI=mongodb://localhost:27017/cleardocs
export REDIS_HOST=localhost
export REDIS_PORT=6379
export JWT_SECRET=VGhpcyBpcyBhIHZlcnkgbG9uZyBzZWNyZXQga2V5IGZvciBDbGVhckRvY3Mgand0IHRva2VucyEhISA=
```

### 3. Run the application

```bash
mvn spring-boot:run
```

---

## Deploying to GitHub & Publishing

### Push to GitHub

```bash
# Initialize git (if not already done)
git init
git add .
git commit -m "feat: initial ClearDocs implementation"

# Create a repo on GitHub, then:
git remote add origin https://github.com/<your-username>/cleardocs.git
git branch -M main
git push -u origin main
```

### GitHub Actions CI/CD

The `.github/workflows/ci.yml` pipeline automatically:
1. **Runs unit tests** on every push/PR
2. **Runs integration tests** with real PostgreSQL, MongoDB, and Redis containers via Testcontainers
3. **Generates JaCoCo coverage report** and uploads to Codecov
4. **Builds a Docker image** on pushes to `main`

To enable it, simply push to GitHub. No additional configuration needed.

### Deploy to a cloud VM (e.g., AWS EC2, GCP GCE, DigitalOcean)

```bash
# On the VM
git clone https://github.com/<your-username>/cleardocs.git
cd cleardocs

# Edit docker-compose.yml to change secrets/passwords for production
# Then:
docker compose -f docker-compose.yml up -d --build
```

### Deploy to Render / Railway

1. Connect your GitHub repo to Render or Railway
2. Set the environment variables from `docker-compose.yml` in the dashboard
3. Use the `Dockerfile` as the build configuration
4. Attach managed PostgreSQL, MongoDB, and Redis add-ons

---

## Project Structure

```
cleardocs/
├── .github/
│   └── workflows/
│       └── ci.yml                    # GitHub Actions CI/CD
├── src/
│   ├── main/
│   │   ├── java/com/cleardocs/
│   │   │   ├── ClearDocsApplication.java
│   │   │   ├── aop/
│   │   │   │   └── AuditAspect.java  # @Audited AOP annotation + controller interceptor
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   └── MongoConfig.java
│   │   │   ├── controller/           # REST controllers
│   │   │   ├── dto/                  # Request/Response DTOs
│   │   │   ├── exception/            # Custom exceptions + GlobalExceptionHandler
│   │   │   ├── model/
│   │   │   │   ├── jpa/              # PostgreSQL entities (User, Document, Workflow)
│   │   │   │   └── mongo/            # MongoDB documents (AuditLog, DocumentMetadata)
│   │   │   ├── repository/
│   │   │   │   ├── jpa/              # Spring Data JPA repositories
│   │   │   │   └── mongo/            # Spring Data MongoDB repositories
│   │   │   ├── security/             # JWT provider, filter, UserPrincipal
│   │   │   ├── service/              # Business logic services
│   │   │   └── statemachine/         # CQRS-influenced document state machine
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/         # Flyway SQL migrations
│   └── test/
│       └── java/com/cleardocs/
│           ├── controller/           # MockMvc web layer tests
│           ├── service/              # Unit tests with Mockito
│           ├── statemachine/         # State machine unit tests
│           └── integration/          # Full-stack Testcontainers tests
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

---

## Security Notes

> **Before deploying to production:**
> 1. Change all default passwords in `docker-compose.yml`
> 2. Generate a new `JWT_SECRET` (min 64 bytes, Base64-encoded)
> 3. Enable HTTPS/TLS (use a reverse proxy like Nginx or Traefik)
> 4. Restrict `management.endpoints.web.exposure.include` to `health,info` only
> 5. Set `MONGO_INITDB_ROOT_USERNAME` and `MONGO_INITDB_ROOT_PASSWORD` for MongoDB auth

---

## License

MIT © ClearDocs Contributors
