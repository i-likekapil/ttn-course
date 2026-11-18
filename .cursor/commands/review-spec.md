# Review spec — Support Ticket System

Run **before** major implementation (and after spec edits). Do not write application code during this review unless the checklist explicitly asks to fix spec files only.

## Spec completeness

- [ ] `spec/requirements.md` — Every assignment feature listed with acceptance mapping.
- [ ] `spec/state-machine.md` — All allowed edges documented; forbidden transitions with expected **409** behavior.
- [ ] `spec/data-model.md` — Ticket + Comment fields, enums, indexes, search approach defined.
- [ ] `spec/api-contract.md` — All endpoints, query params, request/response bodies, status codes.
- [ ] `spec/architecture.md` — Monorepo layout, Java 21, Spring profiles (H2 vs PostgreSQL), Flyway, CORS, OpenAPI.
- [ ] `spec/ui-flow.md` — Routes, forms, filters, error display, status actions aligned with state machine.
- [ ] `spec/test-strategy.md` — Unit, integration (transition matrix), API validation tests described.

## Cross-spec consistency

- [ ] Status values in data model match state machine and API contract enums exactly.
- [ ] `PATCH /api/tickets/{id}` does not allow silent status change unless spec explicitly defines validated status in PATCH (preferred: separate `POST .../status`).
- [ ] Search param name (`q`) and filter param (`status`) consistent in api-contract, ui-flow, and test-strategy.
- [ ] Pagination defaults (`page`, `size`) defined in one place and referenced elsewhere.
- [ ] Comment author field: required vs optional documented for create payload.
- [ ] Priority enum values identical across spec files.

## State machine (assignment gates)

- [ ] OPEN → IN_PROGRESS → RESOLVED → CLOSED path documented.
- [ ] OPEN → CANCELLED and IN_PROGRESS → CANCELLED documented.
- [ ] Examples marked invalid: CLOSED→OPEN, RESOLVED→OPEN, CANCELLED→OPEN (and terminal states cannot leave except as specified).
- [ ] Initial status on create is **OPEN** only.

## Non-functional requirements

- [ ] Persistence across restart called out (H2 file URL and/or Docker PostgreSQL).
- [ ] Error JSON shape sketched or referenced (`api-standards.mdc`).
- [ ] No authentication scope creep unless explicitly added to requirements.

## Alignment with project rules

- [ ] API contract matches `.cursor/rules/api-standards.mdc` (paths, 400/404/409).
- [ ] Test strategy includes mandatory `TicketStatusTransitionIntegrationTest` per `testing.mdc`.

## Gaps and actions

List each gap as:

1. **File** — `spec/...`
2. **Issue** — what is missing or contradictory
3. **Proposed edit** — concrete text or section to add

Apply edits to **spec files only** when fixing gaps. Update `docs/prompt-history.md` after a spec review session.
