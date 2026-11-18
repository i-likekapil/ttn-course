# Spec review — Prompt 9

Reviewed against assignment acceptance criteria and `.cursor/commands/review-spec.md`.  
**Date:** 2026-09-30. No backend/frontend implementation.

## Checklist summary

| Area | Result |
|------|--------|
| Spec completeness (7 files) | **Pass** — all present |
| Cross-spec consistency | **Pass with fixes** — see issues 1–4 (addressed in spec edits) |
| State machine gates | **Pass** |
| Non-functional | **Pass with minor gap** — issue 5 |
| Project rules alignment | **Pass** |

---

## Issues and proposed edits

### 1. `spec/state-machine.md` + `spec/api-contract.md` — same-status transition undefined

**Issue:** State machine says same-status may be 400 or 409; `api-contract.md` did not decide. Implementers and tests could diverge.

**Proposed edit:** In `api-contract.md` under Transition status, add: if `requestedStatus` equals current status → **409** with `detail` like `Ticket is already in status OPEN`. In `state-machine.md`, replace the ambiguous row with a pointer to that rule.

**Status:** Applied in spec files.

---

### 2. `spec/api-contract.md` — `status` on create not specified

**Issue:** `data-model.md` says status on create is ignored or rejected; API only says “must not be sent” without HTTP behavior if client sends it.

**Proposed edit:** Under Create ticket, add: if request body contains `status` → **400**, field error on `status` (“cannot be set on create”).

**Status:** Applied.

---

### 3. `spec/ui-flow.md` — illegal transition demo from terminal state

**Issue:** Acceptance demo asks to attempt illegal transition from `CLOSED`, but UI shows no status buttons for `CLOSED`/`CANCELLED`. Reviewers need a documented path (API tool vs optional dev control).

**Proposed edit:** Add section “Demonstrating 409 (assignment)”: use Swagger/curl for `POST .../status` on a CLOSED ticket, **or** optional read-only “current status” with disabled actions and documented curl example in README (not UI bypass of rules).

**Status:** Applied.

---

### 4. `spec/data-model.md` — dual PK type (`BIGINT` / UUID)

**Issue:** API contract uses `number` for ids; leaving UUID optional invites inconsistent implementations.

**Proposed edit:** Standardize on **BIGINT** (`BIGSERIAL` / `IDENTITY`) for v1; remove UUID alternative.

**Status:** Applied.

---

### 5. `spec/requirements.md` — assignment submission artefacts

**Issue:** Assignment grades spec-driven workflow (`.cursor/`, SpecStory, prompt history, AI review log) but requirements only mention `.cursor/rules` in NFR-3.

**Proposed edit:** Add **NFR-6 Submission artefacts** listing `spec/`, `.specstory/history/`, `docs/prompt-history.md`, `docs/ai-review-log.md`, and steering commands/rules.

**Status:** Applied.

---

### 6. `spec/api-contract.md` — PATCH partial update semantics

**Issue:** “Merge patch” vs “all optional fields” is vague for empty string vs omit vs null for `assignee` and `description`.

**Proposed edit:** Add subsection **PATCH semantics (v1):** omitted keys unchanged; `assignee: null` clears assignee; empty `title` after trim → 400; sending only `{}` → 200 no-op.

**Status:** Applied.

---

### 7. `spec/architecture.md` — Flyway SQL portability

**Issue:** `data-model` uses `TIMESTAMP WITH TIME ZONE`; architecture says “H2 or PostgreSQL dialect” without stating migrations must run on both H2 (default/test) and PostgreSQL (docker).

**Proposed edit:** Under Persistence, add: Flyway scripts must be valid on **H2 and PostgreSQL** (use portable types/functions; avoid PG-only syntax in v1).

**Status:** Applied.

---

### 8. `spec/test-strategy.md` — same-status and create-with-status

**Issue:** Matrix tests did not list same-status or illegal field on create.

**Proposed edit:** Add F4: OPEN→OPEN → 409; API test: POST create with `status` → 400.

**Status:** Applied.

---

### 9. `spec/ui-flow.md` — root route

**Issue:** No landing route; minor UX gap for demo.

**Proposed edit (optional):** `app/page.tsx` redirects to `/tickets` — add one line under Global UX.

**Status:** Applied.

---

### 10. `spec/requirements.md` — GitHub Copilot / dual tooling

**Issue:** Assignment names Cursor **and** GitHub Copilot; specs only imply Cursor.

**Proposed edit (optional):** In NFR-3, note Copilot may be used in IDE alongside Cursor rules committed to repo.

**Status:** Applied in NFR-3 bullet.

---

## No change needed (verified)

- Status enums aligned across data-model, api-contract, state-machine, ui-flow.
- PATCH does not allow status change; dedicated `POST .../status` documented.
- `q`, `status`, `page` (0), `size` (20) consistent.
- Comment `author` and `body` required on create — consistent.
- Priority enum consistent.
- Persistence across restart documented (H2 file + Docker volume).
- `TicketStatusTransitionIntegrationTest` named in test-strategy and testing rules.

---

## Recommended next step

Proceed to **Prompt 10** (backend bootstrap) after skim of applied spec edits.
