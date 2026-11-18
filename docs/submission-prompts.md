# Submission Prompts — Support Ticket Management System

Twenty prompts for spec-driven development (Cursor / GitHub Copilot). Use incrementally; do **not** use a single prompt such as “Build the complete application.”

Export each session to `.specstory/history/` and index entries in `docs/prompt-history.md`.

---

## Phase 0 — Hygiene and workflow

### Prompt 1 — Project steering (Cursor rules)

Create Cursor project rules for this monorepo: Java 21 + Spring Boot 3 in `backend/`, Next.js App Router + TypeScript in `frontend/`. Add `.cursor/rules/` with three `.mdc` files: `java-springboot.mdc` (layered packages, DTOs, `@Valid`, no secrets), `testing.mdc` (JUnit 5, mandatory state-machine integration tests), `api-standards.mdc` (REST naming, query params for search/filter, consistent error JSON). Set `alwaysApply` only where truly global.

### Prompt 2 — Commands and documentation skill

Add `.cursor/commands/review-code.md`, `review-spec.md`, and `generate-tests.md` as checklists tied to the Support Ticket acceptance criteria. Add `.cursor/skills/documentation/SKILL.md` describing when to update `spec/api-contract.md`, OpenAPI, and README. Do not implement application code yet.

### Prompt 3 — Prompt history scaffold

Add `docs/prompt-history.md` (table: date, goal, spec files, outcome) and `docs/ai-review-log.md` (template: AI suggestion, why wrong, fix). Add `.gitignore` entries for `.env`, credentials, and H2/Postgres local data. Document that `.specstory/history/` will hold SpecStory exports.

---

## Phase 1 — Specifications

### Prompt 4 — Requirements

Write `spec/requirements.md` for a Support Ticket Management System: create, list, view detail, update title/description/priority/assignee, add comments, keyword search, filter by status, DB persistence, backend validation, meaningful UI errors. Map each feature to the assignment acceptance criteria.

### Prompt 5 — State machine spec

Write `spec/state-machine.md` as the single source of truth: `OPEN → IN_PROGRESS → RESOLVED → CLOSED`, `OPEN → CANCELLED`, `IN_PROGRESS → CANCELLED`. List forbidden transitions (e.g. `CLOSED → OPEN`, `RESOLVED → OPEN`, `CANCELLED → OPEN`) and require backend rejection with HTTP 409 and a clear message.

### Prompt 6 — Data model

Write `spec/data-model.md`: `Ticket` (id, title, description, priority enum, status enum, assignee string, timestamps) and `Comment` (id, ticketId, body, author, createdAt). Specify indexes for list/filter and search approach (SQL `ILIKE` on title/description for v1).

### Prompt 7 — API contract

Write `spec/api-contract.md` with: `POST /api/tickets`, `GET /api/tickets?status=&q=&page=&size=`, `GET /api/tickets/{id}`, `PATCH /api/tickets/{id}` (fields only, not status), `POST /api/tickets/{id}/status` with `{ "status": "..." }`, `POST /api/tickets/{id}/comments`. Define 400/404/409 error shapes.

### Prompt 8 — Architecture and UI

Write `spec/architecture.md` (monorepo, Spring profiles: H2 default/test, PostgreSQL `docker` profile, Flyway, CORS for Next on :3000, springdoc) and `spec/ui-flow.md` (routes `/tickets`, `/tickets/new`, `/tickets/[id]` with search, filter, edit, status actions, comments). Write `spec/test-strategy.md` including a full transition matrix integration test.

### Prompt 9 — Spec review (no code)

Using `.cursor/commands/review-spec.md`, review all files under `spec/` for gaps against the assignment. List issues and propose concrete edits to the spec files only—do not implement backend or frontend yet.

---

## Phase 2 — Backend

### Prompt 10 — Backend bootstrap

Scaffold `backend/` with Java 21, Spring Boot 3: Web, Validation, Data JPA, Flyway, H2, PostgreSQL driver, springdoc-openapi. Add `application.yml` (H2 file URL so data survives restart on default profile) and `application-docker.yml` plus root `docker-compose.yml` for PostgreSQL. Use env vars via `.env.example` only—no real secrets.

### Prompt 11 — Domain and migrations

Implement JPA entities and Flyway migrations per `spec/data-model.md`. New tickets default to status `OPEN`. Follow `.cursor/rules/java-springboot.mdc`.

### Prompt 12 — Status transition service

Implement `StatusTransitionService` per `spec/state-machine.md` with one central allowed-transition map. Invalid transitions throw a domain exception mapped to HTTP 409. Do not put transition logic in controllers.

### Prompt 13 — State machine integration tests

Per `spec/test-strategy.md` and `.cursor/commands/generate-tests.md`, add `TicketStatusTransitionIntegrationTest` on H2: every allowed edge succeeds; assignment examples of forbidden transitions return 409. Run tests and fix failures without weakening rules.

### Prompt 14 — REST API

Implement REST controllers and DTOs per `spec/api-contract.md`: CRUD/list/search/filter, separate status endpoint, comments endpoint, Bean Validation on inputs, `GlobalExceptionHandler` for 400/404/409. Expose OpenAPI via springdoc.

---

## Phase 3 — Frontend

### Prompt 15 — API client and env

In `frontend/`, add a typed API client for all endpoints in `spec/api-contract.md` using `NEXT_PUBLIC_API_URL`. Parse validation and transition errors from the backend JSON and expose typed errors for UI.

### Prompt 16 — Ticket list and create

Build Next.js pages `/tickets` (list, debounced keyword search, status filter) and `/tickets/new` (create with validation errors shown inline). Follow `spec/ui-flow.md`.

### Prompt 17 — Ticket detail

Build `/tickets/[id]`: view/edit title, description, priority, assignee; show comments; add comment form. Status buttons only for legal next states per `spec/state-machine.md`; still handle 409 from API with a clear message.

---

## Phase 4 — Testing, review, and AI discipline

### Prompt 18 — Backend API tests

Add `@WebMvcTest` or REST tests for: invalid create/update payloads (400), not found (404), search and status filter query params. Do not duplicate the full transition matrix if already covered in integration tests—reference `spec/test-strategy.md`.

### Prompt 19 — Code review pass

Run the checklist in `.cursor/commands/review-code.md` on `backend/` and `frontend/`. Fix: CORS too permissive for production config, validation gaps, status changes bypassing `StatusTransitionService`, or secrets in repo. Summarize changes in `docs/ai-review-log.md` with at least one real AI mistake you caught during the project.

### Prompt 20 — README and spec sync

Update root `README.md`: spec-driven workflow, how to run default (H2) vs `docker` profile (PostgreSQL), how to run tests, OpenAPI URL. Sync `spec/api-contract.md` with implemented API if anything drifted. Append an entry to `docs/prompt-history.md` for this session.

---

## Submission notes

- Record AI corrections in `docs/ai-review-log.md` (wrong transitions, UI-only state machine, insecure defaults, etc.).
- Keep prompts and SpecStory history aligned with the workflow: Requirement → Specification → Plan/Tasks → Implementation → Testing → Review → Fix.
