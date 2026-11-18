# Test strategy — Support Ticket System

Testing proves assignment acceptance criteria and guards the status state machine. Backend tests are **required**; frontend tests are **targeted** (API client + critical UI behavior).

## Test pyramid

```text
        ┌─────────────┐
        │  Manual E2E │  Demo checklist (requirements.md)
        ├─────────────┤
        │  Frontend   │  API client errors (optional component tests)
        ├─────────────┤
        │  Integration│  @SpringBootTest, H2, full HTTP + DB
        ├─────────────┤
        │  Web/API    │  MockMvc / @WebMvcTest slices
        ├─────────────┤
        │  Unit       │  StatusTransitionService matrix
        └─────────────┘
```

## Tooling

| Layer | Tool |
|-------|------|
| Backend | JUnit 5, Spring Boot Test, MockMvc or RestTestClient |
| DB (tests) | H2, `test` profile, Flyway migrations |
| Frontend | Vitest/Jest (optional), fetch mocks for API client |
| CI/local gate | `cd backend && ./mvnw test` must pass |

Reference: `.cursor/rules/testing.mdc`, `.cursor/commands/generate-tests.md`.

---

## 1. Unit tests — `StatusTransitionService`

**Goal:** Fast verification of transition rules without Spring context.

| Test style | Example |
|------------|---------|
| Allowed | `assertTrue(service.canTransition(OPEN, IN_PROGRESS))` |
| Forbidden | `assertThrows` or `assertFalse` for `CLOSED → OPEN` |

Cover **every allowed edge** from `state-machine.md` and a representative set of forbidden pairs.

---

## 2. Integration tests — full transition matrix (mandatory)

**Class name:** `TicketStatusTransitionIntegrationTest`  
**Location:** `backend/src/test/java/.../`  
**Annotations:** `@SpringBootTest`, `@AutoConfigureMockMvc` (or `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`)

**Setup per test (or parameterized):**

1. Create ticket via API or repository (status `OPEN`).
2. Walk allowed transitions with `POST /api/tickets/{id}/status` until target state is reached, **or** use direct DB/repository helper to set current status for forbidden-case tests.
3. Assert HTTP status and persisted `status` on `GET /api/tickets/{id}`.

### Allowed transitions — expect **200** and correct persisted status

| # | From | To | HTTP | Persisted |
|---|------|-----|------|-----------|
| A1 | OPEN | IN_PROGRESS | 200 | IN_PROGRESS |
| A2 | OPEN | CANCELLED | 200 | CANCELLED |
| A3 | IN_PROGRESS | RESOLVED | 200 | RESOLVED |
| A4 | IN_PROGRESS | CANCELLED | 200 | CANCELLED |
| A5 | RESOLVED | CLOSED | 200 | CLOSED |

**Happy path chain (single test):** OPEN → IN_PROGRESS → RESOLVED → CLOSED (all 200).

**Cancel paths:** OPEN → CANCELLED; OPEN → IN_PROGRESS → CANCELLED.

### Forbidden transitions — expect **409**, status unchanged

Minimum assignment examples:

| # | From | To | HTTP | Body |
|---|------|-----|------|------|
| F1 | CLOSED | OPEN | 409 | `detail` mentions CLOSED and OPEN |
| F2 | RESOLVED | OPEN | 409 | same pattern |
| F3 | CANCELLED | OPEN | 409 | same pattern |
| F4 | OPEN | OPEN | 409 | `detail` indicates already in status |

**Recommended additional cases (parameterized):**

| From | To |
|------|-----|
| CLOSED | IN_PROGRESS |
| CANCELLED | RESOLVED |
| OPEN | CLOSED |
| OPEN | RESOLVED |
| IN_PROGRESS | OPEN |
| RESOLVED | CANCELLED |

For each forbidden case: assert `status` on subsequent GET equals **from** state.

### Implementation notes

- Use `@Transactional` only if tests rollback; transition tests often need **commit** between steps — prefer `@SpringBootTest` without class-level `@Transactional` or use `TestRestTemplate` against full context.
- Clear database between tests: `@Sql` scripts, `@DirtiesContext`, or unique data per test.
- Profile: `spring.profiles.active=test` in `src/test/resources/application-test.yml`.

---

## 3. API integration / WebMvc tests

**Classes (suggested):** `TicketControllerTest`, `CommentControllerTest` (slice or full context per team choice).

| Scenario | Method | Expected |
|----------|--------|----------|
| Create valid ticket | POST `/api/tickets` | 201, `status` = OPEN |
| Create missing title | POST | 400, `errors` contains `title` |
| Create with `status` field | POST | 400, `errors` on `status` |
| Get existing | GET `/api/tickets/{id}` | 200, includes `comments` |
| Get missing | GET | 404 |
| PATCH title | PATCH | 200 |
| PATCH includes status field | PATCH | 400 |
| List filter status | GET `?status=OPEN` | only OPEN tickets |
| List search | GET `?q=uniqueToken` | matches title or description |
| Add comment | POST `.../comments` | 201 |
| Comment validation | POST empty body | 400 |

---

## 4. Repository tests (optional)

`@DataJpaTest` for custom search query: insert tickets, assert `q` and `status` combination returns expected rows. Skip if covered by API integration tests only.

---

## 5. Frontend tests (lightweight)

| Target | Assertion |
|--------|-----------|
| API client | Parses 400 `errors[]` into field map |
| API client | Surfaces 409 `detail` as Error message |
| (Optional) List page | Filter change triggers correct query params |

No requirement for full Playwright E2E in v1; manual demo checklist covers UI.

---

## 6. Manual acceptance

Follow **Acceptance verification** in `requirements.md` (11 steps) before submission.

---

## Coverage goals

| Area | Gate |
|------|------|
| State machine | 100% of **allowed** edges + assignment **forbidden** three + optional parameterized illegal set |
| CRUD + comments | At least one automated test per endpoint happy path |
| Validation | At least one 400 per write endpoint type |
| Persistence | Manual restart test (H2 file or Docker volume) documented in README |

---

## What not to do

- Do not delete or weaken transition tests to make builds green.
- Do not test state machine only in the frontend.
- Do not use production PostgreSQL for unit/integration tests without Testcontainers isolation.

---

## Related documents

| Topic | Document |
|-------|----------|
| Transitions | `state-machine.md` |
| API | `api-contract.md` |
| Architecture / profiles | `architecture.md` |
