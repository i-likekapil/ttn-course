# UI flow — Support Ticket System

Next.js App Router frontend. All data from REST API (`api-contract.md`). Base URL: `NEXT_PUBLIC_API_URL`.

## Global UX

- **Navigation:** link from list → create → detail; breadcrumb or back link on detail/new optional.
- **Loading:** show loading state while fetching lists/detail.
- **Errors:** display API `ProblemResponse`:
  - **400:** show `errors[]` next to fields or summary at top.
  - **404:** “Ticket not found” with link back to list.
  - **409:** show `detail` for failed status transition (do not update UI status on failure).
  - **Network:** generic “Could not reach server” message.
- **Enums:** status and priority labels match API strings (`OPEN`, `LOW`, etc.) or friendly labels mapped 1:1.
- **Home:** `/` redirects to `/tickets`.

---

## Route: `/tickets` (list)

**Purpose:** Browse tickets, search, filter by status.

### On load

1. `GET /api/tickets?page=0&size=20` (defaults per API).
2. Render table or card list: title, status, priority, assignee, `updatedAt`.
3. Row click → navigate to `/tickets/[id]`.

### Search

- Search input bound to query param `q` (optional: sync to URL `?q=` for shareable state).
- **Debounce** ~300ms then `GET /api/tickets?q={keyword}&status={filter}&page=0&size=20`.
- Empty search clears `q` and refetches.

### Status filter

- Dropdown or tabs: All | OPEN | IN_PROGRESS | RESOLVED | CLOSED | CANCELLED.
- “All” omits `status` query param; otherwise `GET /api/tickets?status=OPEN&...`.
- Combine with search (AND).

### Pagination (if implemented in UI)

- Next/previous using `page` and `totalPages` from `PagedTicketResponse`.
- Alternatively fixed `size=20` only for v1 minimal UI.

### Actions

- **Create ticket** button → `/tickets/new`.

---

## Route: `/tickets/new` (create)

**Purpose:** Create a ticket (initial status `OPEN` on server).

### Form fields

| Field | Control | Validation (client + server) |
|-------|---------|------------------------------|
| Title | text input | required |
| Description | textarea | required (may be empty if API allows) |
| Priority | select | LOW, MEDIUM, HIGH, URGENT |
| Assignee | text input | optional |

Do **not** expose status on create.

### Submit

1. `POST /api/tickets` with JSON body.
2. **201:** redirect to `/tickets/{id}`.
3. **400:** show field errors from `errors[]`.

### Cancel

- Link back to `/tickets` without saving.

---

## Route: `/tickets/[id]` (detail)

**Purpose:** View and update ticket, change status, add comments.

### On load

1. `GET /api/tickets/{id}` → `TicketDetailResponse`.
2. Render ticket fields and comment thread (`comments` oldest first).

### Edit fields (title, description, priority, assignee)

- Inline form or “Edit” mode.
- **Save:** `PATCH /api/tickets/{id}` with changed fields only.
- **200:** refresh local state from response.
- **400:** show validation errors; **404:** not found page.

Assignee: allow clear (send `assignee: null` if API supports).

### Status actions

Derive visible buttons from current `status` (`state-machine.md`):

| Current status | Actions (call `POST .../status`) |
|----------------|----------------------------------|
| `OPEN` | → IN_PROGRESS, → CANCELLED |
| `IN_PROGRESS` | → RESOLVED, → CANCELLED |
| `RESOLVED` | → CLOSED |
| `CLOSED` | none |
| `CANCELLED` | none |

1. User clicks action → confirm optional → `POST /api/tickets/{id}/status` with `{ "status": "TARGET" }`.
2. **200:** update displayed status.
3. **409:** show `detail` (e.g. illegal transition); keep previous status in UI.

Do not offer PATCH for status.

### Comments

- List existing comments: author, body, `createdAt`.
- **Add comment** form: `body`, `author` (both required).
- Submit → `POST /api/tickets/{id}/comments`.
- **201:** append comment to list or refetch detail.
- **400:** inline errors.

### Navigation

- Link to `/tickets`.
- Optional: after successful update, show brief success hint (non-blocking).

---

## Screen flow diagram

```text
                    ┌─────────────┐
                    │  /tickets   │
                    │  list       │
                    └──────┬──────┘
           ┌───────────────┼───────────────┐
           ▼               ▼               │
    ┌─────────────┐  ┌─────────────┐       │
    │ /tickets/new│  │/tickets/[id]│◄──────┘
    │ create      │  │ detail      │  (row click)
    └──────┬──────┘  └─────────────┘
           │                │
           │ 201 redirect   │ PATCH fields
           └───────────────►│ POST status
                            │ POST comments
```

---

## API mapping summary

| User action | HTTP |
|-------------|------|
| Load list | `GET /api/tickets?...` |
| Create | `POST /api/tickets` |
| Load detail | `GET /api/tickets/{id}` |
| Save fields | `PATCH /api/tickets/{id}` |
| Change status | `POST /api/tickets/{id}/status` |
| Add comment | `POST /api/tickets/{id}/comments` |

---

## Demonstrating invalid transitions (assignment)

Terminal tickets (`CLOSED`, `CANCELLED`) show **no** status buttons in the UI. To satisfy “invalid transition rejected by backend” (e.g. `CLOSED` → `OPEN`):

1. Create a ticket and move it to `CLOSED` via the UI happy path, **or**
2. Call `POST /api/tickets/{id}/status` with Swagger UI (`/swagger-ui/index.html`) or curl with an illegal target.

The UI must still display **409** `detail` if a status action fails; for terminal-state demos, document the curl/Swagger step in README.

---

## Accessibility and polish (recommended)

- Associate labels with inputs; announce errors near forms.
- Disable submit while request in flight to avoid duplicate creates.

---

## Related documents

| Topic | Document |
|-------|----------|
| API shapes | `api-contract.md` |
| Status rules | `state-machine.md` |
| Architecture | `architecture.md` |
| Requirements | `requirements.md` |
