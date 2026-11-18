# Review code — Support Ticket System

Run this checklist after implementation changes or before opening a PR. Mark each item pass/fail; fix failures before merge.

Reference: `.cursor/rules/java-springboot.mdc`, `testing.mdc`, `api-standards.mdc`, and `spec/*`.

## Acceptance criteria (functional)

- [ ] **Create ticket** — UI can create a ticket; API `POST /api/tickets` returns 201; new ticket status is `OPEN`.
- [ ] **List tickets** — UI lists tickets; API `GET /api/tickets` returns expected shape.
- [ ] **View detail** — UI shows single ticket; API `GET /api/tickets/{id}` includes comments where specified.
- [ ] **Update fields** — Title, description, priority updatable via API/UI without changing status rules incorrectly.
- [ ] **Assignee** — Assignee can be set or changed via `PATCH` (or documented endpoint).
- [ ] **Comments** — `POST /api/tickets/{id}/comments` persists and appears on detail view.
- [ ] **Search** — Keyword `q` returns matching tickets (title/description per spec).
- [ ] **Status filter** — `status` query param filters list; UI filter matches backend.
- [ ] **Valid transitions** — OPEN→IN_PROGRESS→RESOLVED→CLOSED; OPEN→CANCELLED; IN_PROGRESS→CANCELLED work end-to-end.
- [ ] **Invalid transitions** — Backend returns **409** (e.g. CLOSED→OPEN, RESOLVED→OPEN, CANCELLED→OPEN); UI shows message, does not fake success.
- [ ] **Persistence** — Data survives app restart (H2 file or PostgreSQL volume per profile).
- [ ] **Backend validation** — Invalid payloads return **400** with field-level or clear `detail`.
- [ ] **UI errors** — Network/validation/409 errors shown meaningfully (not silent failure).

## Architecture and quality

- [ ] Status transition logic lives only in `StatusTransitionService` (or single documented service), not in controllers or UI-only.
- [ ] Controllers use DTOs + `@Valid`; entities are not exposed on the wire.
- [ ] `GlobalExceptionHandler` (or equivalent) maps 400/404/409 to consistent JSON per `api-standards.mdc`.
- [ ] Flyway migrations match JPA entities; no accidental `ddl-auto=create-drop` in committed default config.
- [ ] CORS limited to local Next origin in dev; no wildcard origins in production-oriented config.
- [ ] Frontend uses `NEXT_PUBLIC_API_URL`; no hardcoded production URLs.

## Security and hygiene

- [ ] No secrets, passwords, or API keys in git (check `.env`, YAML, frontend env files).
- [ ] `.gitignore` covers `.env`, local DB files, and credential patterns.
- [ ] `.env.example` documents variable names only (no real values).

## Tests

- [ ] `./mvnw test` passes in `backend/`.
- [ ] State-machine integration test suite passes (mandatory per `testing.mdc`).
- [ ] No tests disabled or assertions weakened to hide bugs.

## Documentation drift

- [ ] If API or run steps changed, `spec/api-contract.md` and README updated (see `.cursor/skills/documentation/SKILL.md`).

## Output

Summarize: **pass count**, **failed items with file/line or endpoint**, and **recommended fixes**. Log any AI-generated code you rejected in `docs/ai-review-log.md`.
