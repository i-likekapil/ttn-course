# Generate tests — Support Ticket System

Use when adding or extending tests. Read `spec/test-strategy.md` and `.cursor/rules/testing.mdc` first. Prefer extending existing test classes over duplicating coverage.

## Mandatory: state machine integration tests

**Class:** `TicketStatusTransitionIntegrationTest` (package under `backend/src/test/...`)

### Allowed transitions (expect success, assert persisted status)

| From | To |
|------|-----|
| OPEN | IN_PROGRESS |
| OPEN | CANCELLED |
| IN_PROGRESS | RESOLVED |
| IN_PROGRESS | CANCELLED |
| RESOLVED | CLOSED |

### Forbidden transitions (expect HTTP 409, status unchanged)

At minimum cover assignment examples:

| From | To |
|------|-----|
| CLOSED | OPEN |
| RESOLVED | OPEN |
| CANCELLED | OPEN |

Add parameterized cases for other illegal edges (e.g. CLOSED→IN_PROGRESS, CANCELLED→RESOLVED) when cheap.

### Implementation notes

- Use `@SpringBootTest` + MockMvc or `TestRestTemplate` with H2 test/default profile.
- Create ticket in known state before each transition attempt.
- Assert response body `detail` (or project error shape) mentions invalid transition when 409.

## API / WebMvc tests

Generate or extend tests for:

- [ ] **Create** — valid 201; missing title → 400.
- [ ] **Update** — valid PATCH; invalid priority → 400.
- [ ] **Get by id** — 200; unknown id → 404.
- [ ] **List** — `?status=OPEN` returns only OPEN tickets.
- [ ] **List** — `?q=keyword` matches title or description per spec.
- [ ] **Comments** — POST adds comment; empty body → 400.
- [ ] **Status endpoint** — valid transition 200; invalid → 409.

## Unit tests

- [ ] `StatusTransitionService` — pure transition map: all allowed/forbidden pairs without Spring context if possible.

## Frontend (when applicable)

- [ ] API client maps 400 `errors[]` and 409 `detail` to types the UI can display.
- [ ] Skip snapshot-only tests unless they catch real regressions.

## After generating tests

1. Run `./mvnw test` in `backend/`.
2. If a test fails, fix **production code** or **spec** — do not delete the test without spec change.
3. Note new test classes in `spec/test-strategy.md` if strategy diverged.

## Checklist before done

- [ ] Transition matrix covered in integration tests.
- [ ] Validation and 404 paths have at least one test each.
- [ ] Tests are deterministic (no shared mutable state across tests).
