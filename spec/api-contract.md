# API contract — Support Ticket System

REST API base URL: `{host}/api` (e.g. `http://localhost:8080/api`).

- **Content-Type:** `application/json` for request and response bodies.
- **Timestamps:** ISO-8601 strings in UTC (e.g. `2026-09-30T10:00:00Z`).
- **Enums:** JSON strings matching `data-model.md` (`TicketStatus`, `Priority`).
- **Status changes:** only via `POST /api/tickets/{id}/status` (see `state-machine.md`). `PATCH` must not accept `status`.

OpenAPI is generated at runtime via springdoc; this file is the human-readable contract.

---

## Shared types

### `TicketResponse`

| Field | Type | Notes |
|-------|------|--------|
| `id` | number | |
| `title` | string | |
| `description` | string | |
| `priority` | string | `LOW` \| `MEDIUM` \| `HIGH` \| `URGENT` |
| `status` | string | `OPEN` \| `IN_PROGRESS` \| `RESOLVED` \| `CLOSED` \| `CANCELLED` |
| `assignee` | string \| null | |
| `createdAt` | string (date-time) | |
| `updatedAt` | string (date-time) | |

### `CommentResponse`

| Field | Type | Notes |
|-------|------|--------|
| `id` | number | |
| `ticketId` | number | |
| `body` | string | |
| `author` | string | |
| `createdAt` | string (date-time) | |

### `TicketDetailResponse`

Extends ticket view with nested comments:

| Field | Type | Notes |
|-------|------|--------|
| (ticket fields) | | Same as `TicketResponse` |
| `comments` | `CommentResponse[]` | Ordered by `createdAt` ascending |

### `PagedTicketResponse`

| Field | Type | Notes |
|-------|------|--------|
| `content` | `TicketResponse[]` | Current page items |
| `page` | number | **0-based** page index |
| `size` | number | Page size |
| `totalElements` | number | Total matching tickets |
| `totalPages` | number | |

---

## Error responses

All errors use the same envelope unless noted.

### `ProblemResponse` (common)

| Field | Type | When present |
|-------|------|----------------|
| `status` | number | HTTP status code (duplicate of response status) |
| `title` | string | Short category |
| `detail` | string | Human-readable explanation |
| `errors` | array | Optional; field-level validation |

### `FieldError`

| Field | Type |
|-------|------|
| `field` | string | DTO property or path |
| `message` | string |

---

### 400 Bad Request — validation

Invalid body, missing required fields, invalid enum string, or malformed JSON.

**HTTP:** `400 Bad Request`

```json
{
  "status": 400,
  "title": "Validation failed",
  "detail": "One or more fields are invalid",
  "errors": [
    { "field": "title", "message": "must not be blank" },
    { "field": "priority", "message": "must not be null" }
  ]
}
```

**Cases:**

- Create/update with blank `title`
- Invalid `priority` value
- Comment with blank `body` or `author`
- `PATCH` body includes `status` → **400** with `field: "status"`, `message: "status cannot be updated via PATCH; use POST /api/tickets/{id}/status"` (or reject unknown properties per Jackson config)

---

### 404 Not Found

Ticket id does not exist (detail, update, status, comments).

**HTTP:** `404 Not Found`

```json
{
  "status": 404,
  "title": "Not found",
  "detail": "Ticket not found with id: 42"
}
```

---

### 409 Conflict — invalid status transition

Requested transition violates `state-machine.md`.

**HTTP:** `409 Conflict`

```json
{
  "status": 409,
  "title": "Conflict",
  "detail": "Cannot transition from CLOSED to OPEN"
}
```

Optional extension (if implemented):

| Field | Type | Notes |
|-------|------|--------|
| `currentStatus` | string | Present status |
| `requestedStatus` | string | Rejected target |

No `errors` array required for 409.

---

## Endpoints

### Create ticket

**`POST /api/tickets`**

**Request body — `CreateTicketRequest`**

| Field | Type | Required | Notes |
|-------|------|----------|--------|
| `title` | string | yes | Not blank |
| `description` | string | yes | May be empty string |
| `priority` | string | yes | Enum `Priority` |
| `assignee` | string | no | Omit or null |

`status` must not be sent; server sets `OPEN`. If the client sends `status` → **400** with `errors[]`: `{ "field": "status", "message": "cannot be set on create" }`.

**Success:** `201 Created`

- Body: `TicketResponse`
- Header: `Location: /api/tickets/{id}` (recommended)

**Errors:** `400` validation

---

### List tickets

**`GET /api/tickets`**

**Query parameters**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `status` | string | no | Exact `TicketStatus` filter |
| `q` | string | no | Keyword search on title and description (`data-model.md`) |
| `page` | integer | no | Default `0`, **0-based** |
| `size` | integer | no | Default `20`, max `100` |

Combine filters with AND: e.g. `status=OPEN&q=login`.

**Success:** `200 OK`

- Body: `PagedTicketResponse`
- Sort: `updatedAt` descending (newest first)

**Errors:** `400` if `status` is not a valid enum (optional strict validation)

---

### Get ticket detail

**`GET /api/tickets/{id}`**

**Path:** `id` — ticket primary key

**Success:** `200 OK`

- Body: `TicketDetailResponse` (ticket + `comments` array)

**Errors:** `404` if ticket missing

---

### Update ticket fields

**`PATCH /api/tickets/{id}`**

Partial update of **fields only**. Omitted JSON properties are left unchanged (JSON merge patch semantics or explicit null rules below).

**Request body — `UpdateTicketRequest`** (all fields optional)

| Field | Type | Notes |
|-------|------|--------|
| `title` | string | If present, not blank |
| `description` | string | If present |
| `priority` | string | If present, valid enum |
| `assignee` | string \| null | If key present, may set or clear assignee |

**Must not include `status`.** If sent → `400` (see validation example above).

**PATCH semantics (v1):**

- Omitted JSON keys leave the field unchanged.
- `assignee: null` clears assignee when the key is present.
- If `title` is present, it must be non-blank after trim → otherwise **400**.
- Empty body `{}` is valid → **200**, no field changes.

**Success:** `200 OK`

- Body: `TicketResponse`

**Errors:** `400` validation, `404` not found

---

### Transition status

**`POST /api/tickets/{id}/status`**

**Request body — `TransitionStatusRequest`**

| Field | Type | Required |
|-------|------|----------|
| `status` | string | yes — target `TicketStatus` |

**Success:** `200 OK`

- Body: `TicketResponse` with new `status`

**Errors:**

- `400` — missing/invalid `status` enum
- `404` — ticket not found
- `409` — illegal transition (`state-machine.md`), including **same status** (target equals current), e.g. `detail`: `Ticket is already in status OPEN`

---

### Add comment

**`POST /api/tickets/{id}/comments`**

**Request body — `CreateCommentRequest`**

| Field | Type | Required |
|-------|------|----------|
| `body` | string | yes — not blank |
| `author` | string | yes — not blank |

**Success:** `201 Created`

- Body: `CommentResponse`
- Header: `Location` optional

**Errors:** `400` validation, `404` ticket not found

---

## Endpoint summary

| Method | Path | Success | Error codes |
|--------|------|---------|-------------|
| POST | `/api/tickets` | 201 | 400 |
| GET | `/api/tickets` | 200 | 400 |
| GET | `/api/tickets/{id}` | 200 | 404 |
| PATCH | `/api/tickets/{id}` | 200 | 400, 404 |
| POST | `/api/tickets/{id}/status` | 200 | 400, 404, 409 |
| POST | `/api/tickets/{id}/comments` | 201 | 400, 404 |

---

## CORS (local development)

Browser clients on `http://localhost:3000` may call this API; server enables CORS for that origin only in dev profile.

---

## Related documents

| Topic | Document |
|-------|----------|
| Fields and search | `data-model.md` |
| Transitions | `state-machine.md` |
| Requirements | `requirements.md` |
| Doc sync | `.cursor/skills/documentation/SKILL.md` |
