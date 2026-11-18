# Data model — Support Ticket System

Logical model for persistence (PostgreSQL / H2 via JPA + Flyway). Status values and transitions are defined in `state-machine.md`; this document defines tables, fields, indexes, and search behavior.

---

## Enumerations

### `TicketStatus` (stored as `VARCHAR`)

| Value | Default on create |
|-------|-------------------|
| `OPEN` | **Yes** |
| `IN_PROGRESS` | |
| `RESOLVED` | |
| `CLOSED` | |
| `CANCELLED` | |

### `Priority` (stored as `VARCHAR`)

| Value | Suggested sort order (list UI) |
|-------|--------------------------------|
| `LOW` | 1 |
| `MEDIUM` | 2 |
| `HIGH` | 3 |
| `URGENT` | 4 |

Exact set is fixed for v1; API and UI use the same string values.

---

## Entity: `Ticket`

Represents one support ticket.

| Field | Type | Constraints | Notes |
|-------|------|-------------|--------|
| `id` | `BIGINT` | PK, generated | Surrogate key; `BIGSERIAL` or `IDENTITY` in Flyway (v1: no UUID) |
| `title` | `VARCHAR(255)` | NOT NULL | Trimmed; non-blank at validation layer |
| `description` | `TEXT` | NOT NULL | May be empty string if product allows; prefer NOT NULL with `""` |
| `priority` | `VARCHAR(32)` | NOT NULL | Enum `Priority` |
| `status` | `VARCHAR(32)` | NOT NULL, default `OPEN` | Enum `TicketStatus`; only changed via status transition flow |
| `assignee` | `VARCHAR(255)` | NULL allowed | Display name or login id; no FK to user table in v1 |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | Set on insert (server clock) |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | Set on insert and every update |

**Table name:** `tickets`

**JPA:** `@Entity` `Ticket`; enums mapped with `@Enumerated(EnumType.STRING)`.

---

## Entity: `Comment`

A single comment on a ticket.

| Field | Type | Constraints | Notes |
|-------|------|-------------|--------|
| `id` | `BIGINT` | PK, generated | |
| `ticket_id` | `BIGINT` | NOT NULL, FK → `tickets.id` | |
| `body` | `TEXT` | NOT NULL | Non-blank at validation layer |
| `author` | `VARCHAR(255)` | NOT NULL | Free text in v1 (no user service) |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | Immutable after insert |

**Table name:** `comments`

**Relationship:** many comments to one ticket. Deleting tickets: **CASCADE** delete comments (or soft-delete out of scope for v1—use `ON DELETE CASCADE`).

**JPA:** `@ManyToOne` `Comment.ticket` with `@JoinColumn(name = "ticket_id")`.

---

## ER diagram

```text
┌─────────────────────────────┐
│ tickets                     │
├─────────────────────────────┤
│ id (PK)                     │
│ title                       │
│ description                 │
│ priority                    │
│ status                      │
│ assignee                    │
│ created_at                  │
│ updated_at                  │
└──────────────┬──────────────┘
               │ 1
               │
               │ *
┌──────────────▼──────────────┐
│ comments                    │
├─────────────────────────────┤
│ id (PK)                     │
│ ticket_id (FK)              │
│ body                        │
│ author                      │
│ created_at                  │
└─────────────────────────────┘
```

---

## Indexes

Support list, filter by status, sort by recency, and ticket lookup by id. Search v1 uses `ILIKE` on title/description (see below).

| Index name | Table | Columns | Purpose |
|------------|-------|---------|---------|
| `pk_tickets` | `tickets` | `id` | Primary key (implicit) |
| `idx_tickets_status` | `tickets` | `status` | Filter `WHERE status = ?` |
| `idx_tickets_updated_at` | `tickets` | `updated_at DESC` | Default list sort (newest first) |
| `idx_tickets_status_updated_at` | `tickets` | `status`, `updated_at DESC` | Filter + sort combined |
| `pk_comments` | `comments` | `id` | Primary key |
| `idx_comments_ticket_id` | `comments` | `ticket_id` | Load comments for detail view |
| `idx_comments_ticket_created` | `comments` | `ticket_id`, `created_at ASC` | Ordered thread per ticket |

**Optional (v1 skip):** full-text (`GIN` / `tsvector` on PostgreSQL) — not required; use `ILIKE` first.

**Flyway:** add indexes in migration `V1__...` or `V2__add_indexes.sql` after table creation.

---

## Search (v1): SQL `ILIKE`

Keyword search is implemented in the repository/service layer when query param `q` is present.

**Behavior:**

- Match if **title** OR **description** contains the keyword (case-insensitive).
- Trim `q`; if blank after trim, ignore search (return unfiltered list subject to `status` filter).
- Escape `%` and `_` in user input if passing to `ILIKE` with wildcards, or use parameterized pattern: `'%' || escape(q) || '%'`.

**H2 / PostgreSQL:**

```sql
-- Conceptual predicate (combine with status filter AND pagination)
WHERE (
  LOWER(title) LIKE LOWER(CONCAT('%', :q, '%'))
  OR LOWER(description) LIKE LOWER(CONCAT('%', :q, '%'))
)
```

**JPA example (Spring Data):**

- Custom `@Query` with `LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))` OR same on `description`.
- Do not use `ILIKE` in JPQL (not portable); `LIKE` + `LOWER()` is sufficient for v1 on H2 and PostgreSQL.

**Performance note:** acceptable for assignment scale; document in `architecture.md` that full-text is a future improvement.

---

## List and filter queries

| Use case | SQL / repository behavior |
|----------|---------------------------|
| List all (paged) | `ORDER BY updated_at DESC` + `LIMIT/OFFSET` or Spring `Pageable` |
| Filter by status | `WHERE status = :status` (+ search AND if `q` set) |
| Get by id | `findById` → 404 if missing |
| Comments for ticket | `WHERE ticket_id = :id ORDER BY created_at ASC` |

---

## Integrity and validation

| Rule | Layer |
|------|--------|
| Title not blank | DTO `@NotBlank` + optional DB NOT NULL |
| Priority required on create/update | DTO `@NotNull` |
| Status on create | Ignored or rejected if sent; entity default `OPEN` |
| Status on update | Only via status endpoint + `state-machine.md` |
| Comment body not blank | DTO `@NotBlank` |
| Comment author not blank | DTO `@NotBlank` |

---

## Migrations

- Versioned Flyway scripts under `backend/src/main/resources/db/migration/`.
- Naming: `V1__create_tickets_and_comments.sql` (or split V1 tables, V2 indexes).
- No `ddl-auto=create-drop` in committed application config.

---

## Related documents

| Topic | Document |
|-------|----------|
| Status transitions | `state-machine.md` |
| REST payloads | `api-contract.md` |
| Requirements | `requirements.md` |
