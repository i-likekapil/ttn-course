# State machine — Ticket status

**Single source of truth** for ticket `status` values and transitions. Backend must enforce this document; UI may guide users but must not be the authority. Implementation: one service (e.g. `StatusTransitionService`) used by the status API only.

## Status enum

| Status | Meaning | Terminal |
|--------|---------|----------|
| `OPEN` | New or not yet worked | No |
| `IN_PROGRESS` | Actively being handled | No |
| `RESOLVED` | Fix/work complete, pending closure | No |
| `CLOSED` | Done | **Yes** |
| `CANCELLED` | Will not be completed | **Yes** |

**On create:** every new ticket is `OPEN`. Clients cannot set another initial status.

## Allowed transitions

Only the following directed edges are legal. Any request to move to a status not listed for the current status is **forbidden**.

```text
OPEN ──────────────► IN_PROGRESS ──────► RESOLVED ──────► CLOSED
  │                        │
  │                        │
  └──────────► CANCELLED ◄─┘
               (from OPEN or IN_PROGRESS only)
```

| From | To | Notes |
|------|-----|--------|
| `OPEN` | `IN_PROGRESS` | Start work |
| `OPEN` | `CANCELLED` | Discard without resolving |
| `IN_PROGRESS` | `RESOLVED` | Work complete |
| `IN_PROGRESS` | `CANCELLED` | Stop work without resolve |
| `RESOLVED` | `CLOSED` | Confirm closure |

**Linear happy path:** `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`.

**Cancel paths:** `OPEN` → `CANCELLED`, `IN_PROGRESS` → `CANCELLED`.

There is **no** transition from `RESOLVED` to `CANCELLED` in v1. There is **no** transition from `CLOSED` or `CANCELLED` to any other status.

## Forbidden transitions (examples)

These must be **rejected by the backend** (assignment examples and common illegal cases):

| From | To | Reject |
|------|-----|--------|
| `CLOSED` | `OPEN` | Yes |
| `RESOLVED` | `OPEN` | Yes |
| `CANCELLED` | `OPEN` | Yes |
| `CLOSED` | `IN_PROGRESS` | Yes |
| `CLOSED` | `RESOLVED` | Yes |
| `CLOSED` | `CANCELLED` | Yes |
| `CANCELLED` | `IN_PROGRESS` | Yes |
| `CANCELLED` | `RESOLVED` | Yes |
| `CANCELLED` | `CLOSED` | Yes |
| `RESOLVED` | `IN_PROGRESS` | Yes |
| `RESOLVED` | `CANCELLED` | Yes |
| `OPEN` | `RESOLVED` | Yes (must go via `IN_PROGRESS`) |
| `OPEN` | `CLOSED` | Yes |
| `IN_PROGRESS` | `OPEN` | Yes (no reopen in v1) |
| Same status | Same status | Yes → **409** with `detail` e.g. `Ticket is already in status OPEN` (`api-contract.md`) |

**Rule:** If `(currentStatus, requestedStatus)` is not in the allowed table, reject.

## API behavior on invalid transition

| Case | HTTP status | Body |
|------|-------------|------|
| Ticket id not found | 404 | Standard not-found problem |
| Illegal transition | **409 Conflict** | `detail` explains forbidden move, e.g. `Cannot transition from CLOSED to OPEN` |
| Valid transition | 200 | Updated ticket representation |

- Use **409** for all illegal state machine edges (including from terminal states).
- `detail` must name **from** and **to** status so the UI can show a meaningful message.
- Do not persist a partial status change on 409.

**Endpoint:** `POST /api/tickets/{id}/status` with body `{ "status": "<TARGET>" }` (see `api-contract.md`). Status must **not** be changed via generic field PATCH without the same rules.

## UI behavior (non-authoritative)

- Show actions only for **legal next statuses** from the current status (e.g. from `OPEN`: “Start progress”, “Cancel”).
- If the API returns 409, display `detail` (or mapped message); do not update local state as if the transition succeeded.

## Implementation checklist

- [ ] Transition map defined in one class (`StatusTransitionService` or equivalent).
- [ ] Controllers delegate to service; no duplicate `if` chains in web layer.
- [ ] Integration tests cover every **allowed** edge and representative **forbidden** edges (minimum: `CLOSED→OPEN`, `RESOLVED→OPEN`, `CANCELLED→OPEN`).
- [ ] `spec/api-contract.md` and OpenAPI stay aligned with this file.

## Reference matrix (implementation)

Allowed targets per current status:

| Current \\ Target | IN_PROGRESS | RESOLVED | CLOSED | CANCELLED |
|-------------------|:-----------:|:--------:|:------:|:---------:|
| OPEN | ✓ | | | ✓ |
| IN_PROGRESS | | ✓ | | ✓ |
| RESOLVED | | | ✓ | |
| CLOSED | | | | |
| CANCELLED | | | | |

Empty cells are **forbidden** → 409.

## Related documents

- Requirements: `requirements.md` (FR-9)
- Tests: `test-strategy.md`, `.cursor/commands/generate-tests.md`
- API: `api-contract.md`
