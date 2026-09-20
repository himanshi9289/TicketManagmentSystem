# Quickstart: Support Ticket Management

Validation guide for the design in [plan.md](./plan.md). Field rules are in [data-model.md](./data-model.md). Request and response shapes are in [contracts/openapi.yaml](./contracts/openapi.yaml).

This guide does not contain implementation code. It is the check to run after the feature is built.

## Prerequisites

- Java 21
- Node.js 20 or newer (for Next.js 16)
- Maven 3.9 or newer
- Optional, for the production profile only: a PostgreSQL 16 database you already run

Do not commit passwords, tokens, or a filled-in `.env` file.

## Environment

Backend, local H2 (data survives restart):

```bash
export SPRING_PROFILES_ACTIVE=h2
export H2_FILE=./data/ticketdb
export DB_DDL_AUTO=update
```

`application.yml` must read those variables and must not embed a PostgreSQL password. Add `data/` to `.gitignore`.

Backend, PostgreSQL profile:

```bash
export SPRING_PROFILES_ACTIVE=postgres
export DB_URL=jdbc:postgresql://localhost:5432/tickets
export DB_USERNAME=tickets
export DB_PASSWORD=replace-me
export DB_DDL_AUTO=update
```

Use a real password only in the environment of the machine that runs the app.

Frontend:

```bash
export BACKEND_URL=http://localhost:8080
```

Next.js rewrites `/api/v1` to `BACKEND_URL`. The browser does not call port 8080 directly.

## Run

From `backend/`:

```bash
mvn spring-boot:run
```

From `frontend/`:

```bash
npm install
npm run dev
```

Open `http://localhost:3000`.

Automated backend checks, from `backend/`:

```bash
mvn test
```

Expected: `TicketStatusMachineTest`, `TicketServiceTest`, and `TicketControllerIntegrationTest` pass. Integration tests use in-memory H2 even when the local app uses a file.

## API checks

Base URL for these calls is `http://localhost:8080/api/v1`. Responses that fail must be JSON with only `timestamp`, `status`, `error`, `message`, and `path`.

1. Create. `POST /tickets` with title, description, priority `HIGH`, and an assignee. Expect HTTP 201, `status` `OPEN`, and a numeric `id`.
2. Create without assignee. Expect HTTP 201 and `assignee` null.
3. Create with a blank title. Expect HTTP 400, no new row, and `message` containing `title`.
4. List. `GET /tickets`. Expect the new tickets, newest `updatedAt` first, without description or comments in each item.
5. Detail. `GET /tickets/{id}`. Expect description, times, and `comments` as an empty array.
6. Allowed path. `PATCH /tickets/{id}/status` with `IN_PROGRESS`, then `RESOLVED`, then `CLOSED`. Each step expects HTTP 200 and the new status.
7. Illegal path. From `CLOSED`, `PATCH` status to `OPEN`. Expect HTTP 400. `message` includes `CLOSED`, `OPEN`, and `The ticket was not changed.` A following `GET` still shows `CLOSED`.
8. Cancel path. Create another ticket and `PATCH` status to `CANCELLED`. Expect HTTP 200. A second ticket in `IN_PROGRESS` can also move to `CANCELLED`. `RESOLVED` to `CANCELLED` expects HTTP 400.
9. Same status. `PATCH` status to the current value. Expect HTTP 200 and the same `updatedAt` as before the call.
10. Edit. On an `OPEN` ticket, `PATCH /tickets/{id}` with a new title. Expect HTTP 200, new title, same status. On the `CLOSED` ticket, the same call expects HTTP 400 and an unchanged title.
11. Comments. `POST /tickets/{id}/comments` on an `OPEN` ticket twice. Detail `comments` are oldest first. The same call on a `CANCELLED` ticket expects HTTP 400 and no new comment.
12. Search. `GET /tickets?keyword=` a word that appears only in a comment. Expect that ticket only. `GET /tickets?status=OPEN` expects only open tickets. Both parameters together require both matches. No match expects HTTP 200 and `[]`. `GET /tickets?status=NOT_A_STATUS` expects HTTP 400 and a message that lists the five statuses, with no Java class name.
13. Missing ticket. `GET /tickets/999999` expects HTTP 404 and the standard error body.

## Restart check

With `H2_FILE` set, create a ticket, stop the backend process, start it again, and `GET` the same id. Title, description, priority, assignee, status, and comments must match. Repeat once with `SPRING_PROFILES_ACTIVE=postgres` if a PostgreSQL instance is available.

## UI checks

Use [contracts/ui.md](./contracts/ui.md).

- Create a ticket in the browser and see it on the list and detail page in under a minute.
- Submit a blank title. The title field stays filled with what you typed, and the screen names title as the problem.
- Move a ticket along the allowed path. Try to reopen a closed ticket. The message alone tells you the current status, the refused status, and that nothing changed.
- Search a comment word and filter by status. Clear both and see every ticket again.
- Stop and start the backend. Reload the UI. The ticket is still there.
