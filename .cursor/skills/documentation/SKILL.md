---
name: documentation
description: >-
  Keeps spec/api-contract.md, OpenAPI (springdoc), and README aligned with the
  Support Ticket API and run instructions. Use when adding or changing REST
  endpoints, error shapes, Spring profiles, Docker setup, frontend env vars, or
  before PR merge when behavior changed.
disable-model-invocation: true
---

# Documentation — Support Ticket monorepo

Keep human-readable spec, generated OpenAPI, and README in sync so reviewers and AI context stay accurate.

## When to update `spec/api-contract.md`

Update **first** (spec-driven) when you:

- Add, rename, or remove an endpoint or HTTP method.
- Change path prefixes, path variables, or query param names (`status`, `q`, `page`, `size`).
- Change request or response JSON fields, enums (`TicketStatus`, `Priority`), or status codes.
- Change error response shape (400 validation, 404, 409 transition).

After implementation, verify the running API matches the spec. If code intentionally diverges, fix code or spec in the same change—do not leave silent drift.

**Do not** document secrets or real connection strings in the api-contract.

## When to update OpenAPI (springdoc)

OpenAPI is **generated from code** once `backend/` exists (springdoc-openapi).

- After controller/DTO changes: run the app and confirm `/v3/api-docs` (or project-configured path) reflects new operations.
- If generated docs disagree with `spec/api-contract.md`, treat as a defect: align code and spec, then re-check Swagger UI.
- README should link to Swagger UI path (e.g. `http://localhost:8080/swagger-ui/index.html`) for local dev only.

Do not hand-maintain a duplicate OpenAPI YAML in repo unless the team explicitly adds one; prefer spec markdown + generated docs.

## When to update `README.md` (repo root)

Update when you change:

- **Prerequisites** — Java 21, Node version, Docker for PostgreSQL profile.
- **Run commands** — `backend/` (`./mvnw spring-boot:run`), `frontend/` (`npm run dev`), profile flags (`spring.profiles.active=docker`).
- **Configuration** — `NEXT_PUBLIC_API_URL`, `.env.example` variables, ports (8080 / 3000).
- **Database** — H2 file location for persistence demo; `docker compose up` for PostgreSQL.
- **Testing** — `./mvnw test`, optional frontend test command.
- **Workflow** — Pointer to `spec/`, `.cursor/commands/`, and `docs/prompt-history.md`.

Keep README concise; deep design stays in `spec/architecture.md`.

## Related files (optional touch)

| Change | Also consider |
|--------|----------------|
| UI routes or flows | `spec/ui-flow.md` |
| Entities or migrations | `spec/data-model.md` |
| Status rules | `spec/state-machine.md` |
| Test scope | `spec/test-strategy.md` |

## Workflow

1. Spec change → `spec/api-contract.md` (and related spec files).
2. Implement backend/frontend.
3. Verify springdoc output vs spec.
4. Update README run/test sections if commands or env vars changed.
5. Add a line to `docs/prompt-history.md` for significant doc updates.

## Quality bar

- New contributor can run backend + frontend from README alone.
- Every public REST operation appears in `spec/api-contract.md` with status codes.
- Assignment acceptance criteria traceable to spec sections (see `spec/requirements.md` when present).
