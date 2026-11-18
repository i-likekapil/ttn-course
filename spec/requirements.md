# Requirements — Support Ticket Management System

## Purpose

A web application for teams to create and manage support tickets: track status through a fixed lifecycle, assign work, discuss via comments, and find tickets via search and filters. Data is stored in a relational database and exposed through a REST API consumed by a Next.js UI.

## Scope

| In scope | Out of scope (v1) |
|----------|-------------------|
| Ticket CRUD (fields listed below), comments, search, status filter | User authentication / RBAC |
| Enforced status state machine on the backend | Email notifications, attachments |
| H2 (default) and PostgreSQL (`docker` profile) | Multi-tenant organizations |
| Backend validation and structured API errors | SLA timers, reporting dashboards |

Detailed API, data model, UI routes, and tests are defined in sibling spec files (`api-contract.md`, `data-model.md`, `ui-flow.md`, `state-machine.md`, `test-strategy.md`).

---

## Functional requirements

### FR-1 Create ticket

- User can create a ticket from the UI with at least: **title**, **description**, **priority**.
- **Assignee** may be set on create or left empty.
- System sets initial **status** to `OPEN` (not user-selectable on create).
- API persists the ticket and returns the created resource.

### FR-2 List tickets

- User can view a list of all tickets (paginated or full list per API contract).
- List shows fields needed to triage: e.g. id, title, status, priority, assignee, updated time.

### FR-3 View ticket detail

- User can open a single ticket and see full **title**, **description**, **priority**, **status**, **assignee**, timestamps.
- User sees all **comments** for that ticket on the detail view.

### FR-4 Update ticket fields

- User can update **title**, **description**, and **priority** on an existing ticket (UI + API).
- Updates must not change **status** except through the dedicated status transition flow (see FR-8).

### FR-5 Change assignee

- User can set or change **assignee** (string identifier) on create or update.
- Clearing assignee is allowed if validation permits empty assignee.

### FR-6 Add comments

- User can add a **comment** (text body; author per API contract) on a ticket detail page.
- Comments are ordered chronologically and persist with the ticket.

### FR-7 Keyword search

- User can search tickets by **keyword**; backend matches **title** and **description** (case-insensitive per architecture).
- Search is available from the list view and via API query parameter `q`.

### FR-8 Filter by status

- User can filter the ticket list by **status** (enum value).
- Filter is applied on the backend; UI filter must use the same values as the API.

### FR-9 Status transitions (state machine)

- User can move a ticket through **allowed** status transitions only.
- **Allowed paths:**
  - `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`
  - `OPEN` → `CANCELLED`
  - `IN_PROGRESS` → `CANCELLED`
- **Invalid transitions** (examples) must be **rejected by the backend**: `CLOSED` → `OPEN`, `RESOLVED` → `OPEN`, `CANCELLED` → `OPEN`, and any other edge not listed as allowed.
- Terminal states (`CLOSED`, `CANCELLED`) do not transition to other statuses in v1.

Authoritative rules: `spec/state-machine.md`.

### FR-10 Database persistence

- Tickets and comments are stored in a database (JPA + Flyway).
- **Default profile:** H2 with file-backed storage so data survives application restart during local development.
- **Docker profile:** PostgreSQL (e.g. via Docker Compose) for environment parity.

---

## Non-functional requirements

### NFR-1 Backend validation

- All write endpoints validate input (Jakarta Bean Validation on DTOs).
- Invalid input returns **HTTP 400** with a consistent error body (field errors where applicable).

### NFR-2 Meaningful UI errors

- UI displays API validation errors (400), not-found (404), and conflict (409) messages without silent failure.
- Network failures show a clear user-visible message.

### NFR-3 API and stack

- **Backend:** Java 21, Spring Boot 3, REST API.
- **Frontend:** Next.js (App Router), TypeScript.
- **Tooling:** Spec-driven development with Cursor and/or GitHub Copilot; committed steering under `.cursor/rules/`, `.cursor/commands/`, and `spec/`.

### NFR-6 Submission artefacts (assignment)

- Specifications under `spec/` maintained before and during implementation.
- Prompt exports in `.specstory/history/` with index in `docs/prompt-history.md`.
- At least three documented AI corrections in `docs/ai-review-log.md`.

### NFR-4 Security hygiene

- No secrets committed to the repository (credentials via environment / `.env.example` only).

### NFR-5 Testability

- State-machine behavior is covered by **integration tests** that must pass in CI/local `./mvnw test` (see `spec/test-strategy.md`).

---

## Traceability — features to assignment acceptance criteria

| ID | Requirement summary | Assignment acceptance criterion |
|----|---------------------|----------------------------------|
| FR-1 | Create ticket (UI + API, status `OPEN`) | Ticket can be created from UI |
| FR-2 | List tickets | Tickets can be listed |
| FR-3 | View detail + comments | Ticket details can be viewed |
| FR-4 | Update title, description, priority | Ticket fields can be updated |
| FR-5 | Change assignee | Assignee can be changed |
| FR-6 | Add comments | Comments can be added |
| FR-7 | Keyword search (`q`) | Search works |
| FR-8 | Filter by status | Status filter works |
| FR-9 (allowed) | Valid transitions | Valid status transitions work |
| FR-9 (forbidden) | 409 on illegal transition | Invalid status transitions are rejected by backend |
| FR-10 | H2 file / PostgreSQL | Data survives application restart |
| NFR-1 | 400 on invalid input | Backend validation works |
| NFR-2 | Show 400/404/409 in UI | UI shows meaningful errors |
| NFR-5 | Transition matrix integration tests | State-machine integration tests pass |
| NFR-4 | `.gitignore`, no secrets in repo | No secrets are committed |

---

## Acceptance verification (demo checklist)

Use this for manual sign-off before submission:

1. Create a ticket from the UI; confirm it appears in the list as `OPEN`.
2. Open detail; edit title, description, priority, assignee; refresh and confirm persistence.
3. Add a comment; confirm it appears on detail.
4. Search by a unique word in title or description; confirm result set.
5. Filter by each status you use in the demo; confirm list matches.
6. Walk happy path: OPEN → IN_PROGRESS → RESOLVED → CLOSED.
7. On a separate ticket, cancel from OPEN or IN_PROGRESS.
8. Attempt an illegal transition (e.g. from CLOSED); confirm API 409 and UI message.
9. Restart backend (and DB volume if applicable); confirm data still present.
10. Submit empty title on create; confirm 400 and UI error display.
11. Run `./mvnw test`; confirm state-machine integration tests pass.

---

## Related specifications

| Topic | Document |
|-------|----------|
| Status rules | `state-machine.md` |
| Entities and enums | `data-model.md` |
| REST endpoints | `api-contract.md` |
| System structure | `architecture.md` |
| Pages and flows | `ui-flow.md` |
| Tests | `test-strategy.md` |
