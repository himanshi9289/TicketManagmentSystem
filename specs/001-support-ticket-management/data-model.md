# Data Model: Support Ticket Management

Persistence and validation for the ticket feature. API field names and status codes are in [contracts/openapi.yaml](./contracts/openapi.yaml).

## Ticket

One support issue.

| Field | Type | Rules |
|-------|------|--------|
| id | Long, generated | Visible reference. Not chosen by the client. Unique. |
| title | String, max 120 | Required after trim. Spaces only are rejected. |
| description | String, max 4000 | Required after trim. Spaces only are rejected. Stored as a long text column. |
| priority | Enum | Required. One of `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`. Stored as the enum name. |
| assignee | String, max 80, nullable | Optional. Blank or spaces become null (unassigned). |
| status | Enum | Set to `OPEN` on create. Client cannot send a starting status. Stored as the enum name. |
| createdAt | Instant | Set once on create. Serialized as ISO-8601. |
| updatedAt | Instant | Set on create and on every successful detail or status change. Comment adds also refresh it so the ticket sorts as recently updated. |
| comments | Collection of Comment | Zero or more. Removed only by deleting the ticket, which this feature does not allow. |

Relationship: one Ticket has many Comments. The comment row holds the ticket id. Deleting a ticket is out of scope, so comments are not cascade-deleted by an API.

List ordering: `updatedAt` descending. The list projection omits `description` and comments.

## Comment

A note on one ticket. Not editable and not deletable.

| Field | Type | Rules |
|-------|------|--------|
| id | Long, generated | Identity of the comment. |
| ticket | Ticket | Required. Many comments belong to one ticket. |
| authorName | String, max 80 | Required after trim. Spaces only are rejected. |
| text | String, max 2000 | Required after trim. Spaces only are rejected. |
| createdAt | Instant | Set once. Detail view orders comments by `createdAt` ascending. |

A comment can be added only when the ticket status is `OPEN`, `IN_PROGRESS`, or `RESOLVED`.

## Enums

**TicketPriority**: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.

The UI labels are Low, Medium, High, and Critical. The API uses the enum names above.

**TicketStatus**: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`.

The UI labels are Open, In Progress, Resolved, Closed, and Cancelled.

## Status transitions

Enforced only by `TicketStatusMachine` in the service layer.

| From | Allowed to |
|------|------------|
| OPEN | IN_PROGRESS, CANCELLED |
| IN_PROGRESS | RESOLVED, CANCELLED |
| RESOLVED | CLOSED |
| CLOSED | none |
| CANCELLED | none |

Requesting the current status does not write a new status and is not an error. `updatedAt` stays unchanged in that case.

Any other pair is rejected. The stored row is unchanged. The error message includes the current status, the requested status, and the sentence "The ticket was not changed."

These pairs are rejected even though some are already implied by the table: Open to Resolved, Open to Closed, In Progress to Open, In Progress to Closed, Resolved to Open, Resolved to In Progress, Resolved to Cancelled, and every move out of Closed or Cancelled.

## Edit rules that are not transitions

| Status | Change title, description, priority, assignee | Add comment |
|--------|-----------------------------------------------|-------------|
| OPEN | Allowed | Allowed |
| IN_PROGRESS | Allowed | Allowed |
| RESOLVED | Allowed | Allowed |
| CLOSED | Rejected, row unchanged | Rejected, no row inserted |
| CANCELLED | Rejected, row unchanged | Rejected, no row inserted |

A successful detail change does not modify `status`.

## Search

`keyword` is optional. When present and not blank, a ticket matches if the keyword appears, case-insensitive, in `title`, `description`, or any comment `text`. The match is a substring. The characters `%` and `_` are literal, not wildcards.

`status` is optional. When present it must be one of the five statuses. Combined with `keyword`, both conditions apply.

Both absent: every ticket, newest update first.

No match: an empty list and HTTP 200. The UI says that nothing matched. That is not an error response.

An unknown `status` value: HTTP 400, standard error body, message lists the five allowed names. No class name in the message.

## Validation messages

Bean Validation runs before the service saves. The handler turns each violation into `field: reason` and joins them with `; ` inside `message`. Nothing is saved.

Examples of reasons the message must be able to carry:

- title missing or blank
- title longer than 120
- description missing, blank, or longer than 4000
- priority missing or not one of the four values
- assignee longer than 80
- comment author missing, blank, or longer than 80
- comment text missing, blank, or longer than 2000
- keyword longer than 200

## What is stored across restarts

On the H2 file database and on PostgreSQL, Ticket and Comment rows, including status, remain after the process stops and starts. A rejected request does not insert or update a row. Tests may use in-memory H2; local development must not, or the restart check in the spec fails.
