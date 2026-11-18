# AI review log

Record cases where AI output was **wrong or unsafe** and you corrected it. This shows AI used as an engineering assistant, not blind acceptance.

Assignment: include **at least three** meaningful entries before submission.

---

## Entry template

Copy this block for each incident:

### Entry N — Short title

| Field | Notes |
|-------|--------|
| **Date** | YYYY-MM-DD |
| **Context** | Prompt, file, or feature (e.g. status transition API) |
| **AI suggestion** | What the model proposed |
| **Why wrong** | Incorrect behavior, spec violation, security, or maintainability issue |
| **Fix** | What you changed (file paths, approach) |
| **Verified by** | Test name, manual step, or review command |

---

## Entries

_No entries yet. Add your first after rejecting or correcting AI-generated code._

<!-- Example (delete or replace when you have real entries):

### Entry 1 — Status change in PATCH handler

| Field | Notes |
|-------|--------|
| **Date** | 2026-10-01 |
| **Context** | Prompt 14 — REST API |
| **AI suggestion** | Allow `status` on `PATCH /api/tickets/{id}` with no transition rules |
| **Why wrong** | Bypasses `spec/state-machine.md`; invalid transitions could reach DB |
| **Fix** | Dedicated `POST /api/tickets/{id}/status` + `StatusTransitionService` only |
| **Verified by** | `TicketStatusTransitionIntegrationTest` |

-->
