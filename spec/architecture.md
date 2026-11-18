# Architecture — Support Ticket System

## Overview

Monorepo with a **Spring Boot** REST backend and a **Next.js** (App Router) frontend. The frontend calls the backend over HTTP; there is no server-side BFF in v1. Business rules (especially ticket status) are enforced only on the backend.

```text
┌──────────────────┐     HTTP/JSON      ┌─────────────────────────────┐
│  frontend/       │ ─────────────────► │  backend/                   │
│  Next.js :3000   │   NEXT_PUBLIC_     │  Spring Boot :8080          │
│                  │   API_URL          │  /api/*                     │
└──────────────────┘                    └──────────────┬──────────────┘
                                                     │
                                                     ▼
                                          ┌─────────────────────┐
                                          │  H2 (default) or    │
                                          │  PostgreSQL (docker)│
                                          └─────────────────────┘
```

## Repository layout

| Path | Responsibility |
|------|----------------|
| `backend/` | Java 21, Spring Boot 3, JPA, Flyway, REST, springdoc |
| `frontend/` | Next.js App Router, TypeScript, ticket UI |
| `spec/` | Specifications (requirements, API, data model, etc.) |
| `docs/` | Prompt history, AI review log, submission docs |
| `.cursor/` | Rules, commands, skills for AI-assisted development |
| `docker-compose.yml` | PostgreSQL for `docker` Spring profile (root level) |

## Backend structure

Suggested package layout under `com.<org>.tickets`:

| Package | Contents |
|---------|----------|
| `domain` | JPA entities, enums |
| `repository` | Spring Data interfaces |
| `service` | `TicketService`, `CommentService`, `StatusTransitionService` |
| `web` | REST controllers, request/response DTOs |
| `web.advice` | `GlobalExceptionHandler` → `ProblemResponse` |
| `config` | CORS, Jackson, optional OpenAPI beans |

**Principles:** controllers stay thin; DTOs at the boundary; status transitions only through `StatusTransitionService` (`state-machine.md`).

## Frontend structure

Suggested under `frontend/`:

| Path | Contents |
|------|----------|
| `app/tickets/page.tsx` | List, search, filter |
| `app/tickets/new/page.tsx` | Create ticket |
| `app/tickets/[id]/page.tsx` | Detail, edit, status, comments |
| `lib/api/` | Typed client for `api-contract.md` |
| `components/` | Reusable form, error banner, status actions |

Environment: `NEXT_PUBLIC_API_URL=http://localhost:8080/api` (`.env.local`, documented in `.env.example`).

## Persistence and migrations

- **ORM:** Spring Data JPA (`data-model.md`).
- **Schema:** Flyway SQL in `backend/src/main/resources/db/migration/`.
- **Portability:** Migrations must run on **H2** (default/test) and **PostgreSQL** (`docker`); avoid PostgreSQL-only syntax in v1 scripts.
- **DDL:** `spring.jpa.hibernate.ddl-auto=validate` (or `none`) in committed config; never rely on auto-ddl for production-like profiles.

### Spring profiles

| Profile | When | Database | Purpose |
|---------|------|----------|---------|
| *(default)* | Local dev, demo persistence | **H2 file** (`./data/ticketdb` or similar) | Data survives restart; no Docker required |
| `test` | `./mvnw test` | **H2 in-memory** (or test file) | Fast, isolated tests |
| `docker` | `spring.profiles.active=docker` + Compose | **PostgreSQL** | Parity with deployed DB |

**Default `application.yml` (conceptual):**

- H2 file URL, PostgreSQL dialect compatibility or H2 dialect for default.
- Flyway enabled on startup.

**`application-docker.yml`:**

- Datasource from env: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (values in `.env.example` only).

**`application-test.yml`:**

- In-memory H2, Flyway migrate, optional `spring.jpa.show-sql=false`.

Root **`docker-compose.yml`:** PostgreSQL service, port `5432`, volume for data persistence across container restarts.

## API documentation (springdoc)

- Dependency: `springdoc-openapi-starter-webmvc-ui`.
- **OpenAPI JSON:** `/v3/api-docs`
- **Swagger UI:** `/swagger-ui/index.html` (local: `http://localhost:8080/swagger-ui/index.html`)

Keep controllers annotated or rely on default scanning; verify generated docs match `spec/api-contract.md` after changes (see `.cursor/skills/documentation/SKILL.md`).

## CORS

- **Dev:** allow origin `http://localhost:3000`, methods `GET, POST, PATCH, OPTIONS`, headers `Content-Type`.
- Configure in `config` package; restrict origins per profile (no `*` in docker/production-oriented settings).

## Error and validation flow

```text
Controller (@Valid DTO)
    → Service (business rules, transitions)
        → Repository
    ← Domain exceptions → GlobalExceptionHandler
        → 400 / 404 / 409 ProblemResponse (api-contract.md)
```

## Search implementation

Keyword `q` on list endpoint: repository query with `LOWER(title) LIKE` / `LOWER(description) LIKE` (`data-model.md`). No Elasticsearch in v1.

## Security (v1)

- No authentication; assignee and author are free-text fields.
- No secrets in git; use environment variables for DB credentials in `docker` profile.

## Token / AI context (optional)

For large implementations, optional MCP tools (e.g. codebase memory, graph-based context) may be documented in README to reduce repeated spec paste. Not required for correctness.

## Local run (reference)

| Component | Command | URL |
|-----------|---------|-----|
| Backend (default H2) | `cd backend && ./mvnw spring-boot:run` | http://localhost:8080 |
| Backend (PostgreSQL) | `docker compose up -d` then `./mvnw spring-boot:run -Dspring-boot.run.profiles=docker` | same |
| Frontend | `cd frontend && npm run dev` | http://localhost:3000 |
| Tests | `cd backend && ./mvnw test` | — |

## Related documents

| Topic | Document |
|-------|----------|
| API | `api-contract.md` |
| UI | `ui-flow.md` |
| Tests | `test-strategy.md` |
| Status rules | `state-machine.md` |
