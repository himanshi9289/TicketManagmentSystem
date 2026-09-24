---

description: "Task list for Support Ticket Management implementation"
---

# Tasks: Support Ticket Management

**Input**: Design documents from `/specs/001-support-ticket-management/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Included. The constitution (Principle IV and Quality Gates) requires JUnit 5, Mockito, and H2 integration tests for this feature. Write each story's tests first and confirm they fail before implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Web app**: `backend/src/`, `frontend/src/`
- Paths below follow `specs/001-support-ticket-management/plan.md`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Create `backend/` and `frontend/` directory trees from the Project Structure section of `specs/001-support-ticket-management/plan.md`
- [ ] T002 [P] Create `backend/pom.xml` with Java 21, parent Spring Boot 3.5.16, and dependencies spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-validation, Lombok, H2, and the PostgreSQL driver
- [X] T003 [P] Create the Next.js 16.3.5 + React 19 + TypeScript app files `frontend/package.json`, `frontend/tsconfig.json`, and `frontend/src/app/layout.tsx` using the App Router
- [ ] T004 [P] Add `data/` to `.gitignore` so the local H2 file database is never committed

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T005 Create the Spring Boot entry point in `backend/src/main/java/com/ticketmanagement/TicketManagementApplication.java`
- [ ] T006 [P] Create `backend/src/main/java/com/ticketmanagement/domain/TicketStatus.java` with exactly `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`
- [ ] T007 [P] Create `backend/src/main/java/com/ticketmanagement/domain/TicketPriority.java` with exactly `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- [ ] T008 [P] Add env-only config in `backend/src/main/resources/application.yml` and `backend/src/main/resources/application-postgres.yml`: default profile uses file H2 (`MODE=PostgreSQL`) from `H2_FILE`, `DB_DDL_AUTO` supplies `spring.jpa.hibernate.ddl-auto`, and the postgres profile reads `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` with no password default and no committed secret. Add `backend/src/test/resources/application.yml` using in-memory H2 for tests
- [ ] T009 [P] Create `backend/src/main/java/com/ticketmanagement/dto/ApiErrorResponse.java` and `backend/src/main/java/com/ticketmanagement/config/ApiErrorHandler.java` so every error JSON has only `timestamp`, `status`, `error`, `message`, and `path`. Join validation failures as `field: reason` segments separated by `; `. Do not include stack traces, SQL, or Java class names
- [X] T010 [P] Proxy `/api/v1` to `BACKEND_URL` in `frontend/next.config.ts` (no permissive CORS on the API)
- [X] T011 [P] Define ticket, comment, and `ApiError` types in `frontend/src/lib/types.ts` matching `specs/001-support-ticket-management/contracts/openapi.yaml`
- [X] T012 [P] Create `frontend/src/components/ApiErrorMessage.tsx` to show only the error `message`
- [X] T013 Implement `frontend/src/lib/api.ts` with typed calls for the `/api/v1` ticket operations, surfacing the five-field error body to callers

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Create, list, and view a ticket (Priority: P1) 🎯 MVP

**Goal**: A user can save a ticket (title, description, priority, optional assignee) that starts as Open, see it in the list and on its detail view, and still see it after a restart.

**Independent Test**: Create one ticket, confirm it appears in the list and on its detail view with the entered values and status Open, restart the application, and confirm the same ticket is unchanged.

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T014 [P] [US1] Add failing Mockito tests in `backend/src/test/java/com/ticketmanagement/service/TicketServiceTest.java` for create (status forced to `OPEN`, blank assignee stored as null), list ordered by `updatedAt` descending, and get-by-id including not-found
- [ ] T015 [P] [US1] Add failing H2 MockMvc tests in `backend/src/test/java/com/ticketmanagement/controller/TicketControllerIntegrationTest.java` for `POST /api/v1/tickets`, `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}`, blank-title 400 whose `message` contains `title`, and a 404 whose body has only `timestamp`, `status`, `error`, `message`, and `path`

### Implementation for User Story 1

- [ ] T016 [P] [US1] Create `backend/src/main/java/com/ticketmanagement/entity/Ticket.java` (Lombok `@Getter` `@Setter` `@NoArgsConstructor` `@Builder`, not `@Data`) with: id Long generated, visible reference, not chosen by the client, unique; title String max 120, required after trim, spaces only are rejected; description String max 4000, required after trim, spaces only are rejected, long text column; priority enum required, one of `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`, stored as the enum name; assignee String max 80, nullable, blank or spaces become null; status enum stored as the enum name; createdAt Instant set once; updatedAt Instant set on create
- [ ] T017 [P] [US1] Create records `backend/src/main/java/com/ticketmanagement/dto/CreateTicketRequest.java`, `backend/src/main/java/com/ticketmanagement/dto/TicketSummaryResponse.java`, and `backend/src/main/java/com/ticketmanagement/dto/TicketDetailResponse.java`. Create body requires title (min 1, max 120), description (min 1, max 4000), and priority; assignee is optional max 80. Summary omits description and comments. Detail includes an empty comments list. No entity type appears in these records
- [ ] T018 [P] [US1] Create `backend/src/main/java/com/ticketmanagement/exception/TicketNotFoundException.java` with a message that does not include a class name
- [ ] T019 [US1] Create `backend/src/main/java/com/ticketmanagement/repository/TicketRepository.java` with `findAll` ordered by `updatedAt` descending
- [ ] T020 [US1] Create `backend/src/main/java/com/ticketmanagement/service/TicketMapper.java` to map between `Ticket` and the US1 DTOs. Mapping is invoked only from the service
- [ ] T021 [US1] Implement create, list, and getById in `backend/src/main/java/com/ticketmanagement/service/TicketService.java`. New status is always `OPEN`. The client cannot send a starting status. Trim text; spaces-only title or description is rejected; blank assignee becomes null. Set `createdAt` and `updatedAt` on create
- [ ] T022 [US1] Implement `POST /api/v1/tickets`, `GET /api/v1/tickets`, and `GET /api/v1/tickets/{id}` in `backend/src/main/java/com/ticketmanagement/controller/TicketController.java`. Controller methods accept and return DTOs only, contain no business rules, and do not call the repository
- [X] T023 [P] [US1] Build the ticket list in `frontend/src/app/page.tsx` showing reference, title, status, priority, assignee or Unassigned, and last updated time, newest first, plus a link to create
- [X] T024 [P] [US1] Build the create form in `frontend/src/app/tickets/new/page.tsx` for title, description, priority, and optional assignee, with no status control. On 400, keep the typed values and show each `field: reason` from `message`
- [X] T025 [P] [US1] Build the read-only detail view in `frontend/src/app/tickets/[id]/page.tsx` showing reference, title, description, priority, assignee or Unassigned, status, created time, updated time, and no comments yet

**Checkpoint**: User Story 1 is functional and testable on its own. File H2 (`H2_FILE`) must still return the ticket after a process restart.

---

## Phase 4: User Story 2 - Move a ticket only along the allowed path (Priority: P1)

**Goal**: Status changes follow only the five allowed steps. Every other change is rejected, the stored status stays the same, and the screen explains why.

**Independent Test**: Starting from a saved Open ticket, perform each allowed step and confirm the new status. Then attempt each forbidden step, including any return to Open, and confirm the status does not change and a clear error is shown.

### Tests for User Story 2 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T026 [P] [US2] Add failing JUnit 5 tests in `backend/src/test/java/com/ticketmanagement/service/TicketStatusMachineTest.java` for the five allowed steps, the same-status no-op, and rejected pairs including `CLOSED` to `OPEN`, `RESOLVED` to `OPEN`, `CANCELLED` to `OPEN`, `OPEN` to `RESOLVED`, `OPEN` to `CLOSED`, `IN_PROGRESS` to `OPEN`, `IN_PROGRESS` to `CLOSED`, and `RESOLVED` to `CANCELLED`
- [ ] T027 [P] [US2] Add failing H2 MockMvc tests in `backend/src/test/java/com/ticketmanagement/controller/TicketControllerIntegrationTest.java` for `PATCH /api/v1/tickets/{id}/status`: each allowed step returns 200, an illegal step returns 400, the GET after rejection still shows the previous status, and `message` includes the current status, the requested status, and `The ticket was not changed.`

### Implementation for User Story 2

- [ ] T028 [P] [US2] Implement `backend/src/main/java/com/ticketmanagement/service/TicketStatusMachine.java` allowing only `OPEN` → `IN_PROGRESS`, `OPEN` → `CANCELLED`, `IN_PROGRESS` → `RESOLVED`, `IN_PROGRESS` → `CANCELLED`, and `RESOLVED` → `CLOSED`. The same status is not an error. Every other pair is rejected before any save
- [ ] T029 [P] [US2] Create `backend/src/main/java/com/ticketmanagement/exception/InvalidStatusTransitionException.java` whose message includes the current status, the requested status, and the sentence `The ticket was not changed.`
- [ ] T030 [P] [US2] Create `backend/src/main/java/com/ticketmanagement/dto/UpdateStatusRequest.java` with required `status` and no other properties
- [ ] T031 [US2] Add `changeStatus` to `backend/src/main/java/com/ticketmanagement/service/TicketService.java` so it calls `TicketStatusMachine` and does not persist when the transition is rejected. A same-status request does not change `updatedAt`
- [ ] T032 [US2] Add `PATCH /api/v1/tickets/{id}/status` to `backend/src/main/java/com/ticketmanagement/controller/TicketController.java` that only forwards the requested status and does not encode transition rules
- [X] T033 [US2] Add status actions to `frontend/src/app/tickets/[id]/page.tsx`. After a 400, show `message` unchanged and reload the ticket so the on-screen status matches the stored status

**Checkpoint**: User Stories 1 and 2 both work. Status rules are unit-testable without the controller.

---

## Phase 5: User Story 3 - Update ticket details (Priority: P2)

**Goal**: Title, description, priority, and assignee can change while the ticket is Open, In Progress, or Resolved. Closed and Cancelled tickets cannot be edited. A detail update does not change status.

**Independent Test**: Change each editable field on an Open ticket and confirm the detail view shows the new values and the same status. Repeat the attempt on a Closed ticket and confirm nothing changes and an error is shown.

### Tests for User Story 3 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T034 [P] [US3] Add failing Mockito tests in `backend/src/test/java/com/ticketmanagement/service/TicketServiceTest.java` for updates in `OPEN`, `IN_PROGRESS`, and `RESOLVED` (status unchanged, null assignee clears it) and for rejection in `CLOSED` and `CANCELLED` with the stored row unchanged
- [ ] T035 [P] [US3] Add failing H2 MockMvc tests in `backend/src/test/java/com/ticketmanagement/controller/TicketControllerIntegrationTest.java` for `PATCH /api/v1/tickets/{id}` covering a successful title change, a 400 on a Closed ticket, and a 400 when title is present but blank

### Implementation for User Story 3

- [ ] T036 [P] [US3] Create `backend/src/main/java/com/ticketmanagement/dto/UpdateTicketRequest.java`. At least one of title, description, priority, or assignee must be present. Title, if present, is min 1 max 120. Description, if present, is min 1 max 4000. Priority, if present, is one of `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`. Assignee max 80; JSON null clears it; omitting it leaves it unchanged
- [ ] T037 [P] [US3] Create `backend/src/main/java/com/ticketmanagement/exception/TicketNotEditableException.java` with a message that a finished ticket cannot be edited and the ticket was not changed
- [ ] T038 [US3] Add `updateDetails` to `backend/src/main/java/com/ticketmanagement/service/TicketService.java`. Allow edits only in `OPEN`, `IN_PROGRESS`, and `RESOLVED`. Reject `CLOSED` and `CANCELLED` without writing. Do not change `status`. Refresh `updatedAt` only on success
- [ ] T039 [US3] Add `PATCH /api/v1/tickets/{id}` to `backend/src/main/java/com/ticketmanagement/controller/TicketController.java` using `UpdateTicketRequest` and `TicketDetailResponse` only
- [X] T040 [US3] Add the edit form to `frontend/src/app/tickets/[id]/page.tsx`. Disable it when status is Closed or Cancelled. On 400, keep the typed values and show the field errors from `message`

**Checkpoint**: Detail edits work on active tickets and are refused on finished tickets, without changing status.

---

## Phase 6: User Story 4 - Add comments (Priority: P2)

**Goal**: A user can add a named comment to an Open, In Progress, or Resolved ticket. Comments show oldest first and survive a restart. Closed and Cancelled tickets reject new comments.

**Independent Test**: Add two comments to an Open ticket and confirm both appear in order with name and text. Attempt a comment on a Cancelled ticket and confirm it is refused.

### Tests for User Story 4 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T041 [P] [US4] Add failing Mockito tests in `backend/src/test/java/com/ticketmanagement/service/TicketServiceTest.java` for two comments returned oldest first, rejection on `CLOSED` and `CANCELLED` with no insert, and `updatedAt` on the ticket changing when a comment is saved
- [ ] T042 [P] [US4] Add failing H2 MockMvc tests in `backend/src/test/java/com/ticketmanagement/controller/TicketControllerIntegrationTest.java` for `POST /api/v1/tickets/{id}/comments` (201, then detail order oldest first), blank author 400 whose `message` names the field, and 400 on a Cancelled ticket with the comment count unchanged

### Implementation for User Story 4

- [ ] T043 [P] [US4] Create `backend/src/main/java/com/ticketmanagement/entity/Comment.java` and add the comments collection on `backend/src/main/java/com/ticketmanagement/entity/Ticket.java`. Comment fields: id Long generated; ticket required; authorName String max 80, required after trim, spaces only are rejected; text String max 2000, required after trim, spaces only are rejected; createdAt Instant set once. Comments are not editable or deletable
- [ ] T044 [P] [US4] Create `backend/src/main/java/com/ticketmanagement/dto/CreateCommentRequest.java` (authorName min 1 max 80, text min 1 max 2000) and `backend/src/main/java/com/ticketmanagement/dto/CommentResponse.java` (id, authorName, text, createdAt)
- [ ] T045 [P] [US4] Create `backend/src/main/java/com/ticketmanagement/exception/CommentNotAllowedException.java` stating a finished ticket cannot receive comments
- [ ] T046 [US4] Create `backend/src/main/java/com/ticketmanagement/repository/CommentRepository.java` to load comments for a ticket ordered by `createdAt` ascending
- [ ] T047 [US4] Add `addComment` in `backend/src/main/java/com/ticketmanagement/service/TicketService.java` and return comments oldest first from `backend/src/main/java/com/ticketmanagement/service/TicketMapper.java`. Allow comments only for `OPEN`, `IN_PROGRESS`, and `RESOLVED`. On success, set the ticket `updatedAt` to the comment time
- [ ] T048 [US4] Add `POST /api/v1/tickets/{id}/comments` to `backend/src/main/java/com/ticketmanagement/controller/TicketController.java` returning 201 and `CommentResponse`
- [X] T049 [US4] Show comments oldest first and add the comment form on `frontend/src/app/tickets/[id]/page.tsx`. Disable the form for Closed and Cancelled. On 400, keep the typed name and text

**Checkpoint**: Comments persist, stay in oldest-first order, and are refused on finished tickets.

---

## Phase 7: User Story 5 - Search by keyword and filter by status (Priority: P2)

**Goal**: The list narrows by a case-insensitive keyword, by one status, or by both. No match is an empty list, not an error. An unknown status filter is rejected.

**Independent Test**: Create tickets with distinct titles, descriptions, comments, and statuses. Search for a word that appears only in a comment, filter to one status, then combine both, and confirm the list matches only the expected tickets.

### Tests for User Story 5 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T050 [P] [US5] Add failing tests in `backend/src/test/java/com/ticketmanagement/service/TicketServiceTest.java` for keyword match on title, description, and comment text ignoring case, `%` and `_` treated as literal characters, status filter AND keyword, blank keyword meaning no keyword restriction, and an unknown status rejected with the five allowed names in the message
- [ ] T051 [P] [US5] Add failing H2 MockMvc tests in `backend/src/test/java/com/ticketmanagement/controller/TicketControllerIntegrationTest.java` for `GET /api/v1/tickets?keyword=`, `GET /api/v1/tickets?status=OPEN`, both parameters together, no-match HTTP 200 with `[]`, and `status=NOT_A_STATUS` HTTP 400 whose `message` lists `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED` and contains no Java class name

### Implementation for User Story 5

- [ ] T052 [US5] Add a search query to `backend/src/main/java/com/ticketmanagement/repository/TicketRepository.java`: case-insensitive substring on title, description, or any comment text; `%` and `_` are literal; optional status must match exactly; both conditions apply when both are present; results ordered by `updatedAt` descending
- [ ] T053 [US5] Apply keyword and status in `backend/src/main/java/com/ticketmanagement/service/TicketService.java`. Keyword longer than 200 characters is rejected. A blank keyword is ignored. An unknown status is a 400 whose message lists the five statuses. No match returns an empty list, not an error
- [ ] T054 [US5] Accept optional `keyword` and `status` query parameters on `GET /api/v1/tickets` in `backend/src/main/java/com/ticketmanagement/controller/TicketController.java` and pass them to the service without filtering in the controller
- [X] T055 [US5] Add the keyword box and status filter to `frontend/src/app/page.tsx`. When nothing matches, show that no tickets match and do not use `ApiErrorMessage`. Clearing both controls lists every ticket

**Checkpoint**: Search and filter match the spec, including a keyword that appears only in a comment.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T056 [P] Review `backend/src/main/resources/application.yml` and `backend/src/main/resources/application-postgres.yml` and remove any hardcoded password, token, or real connection secret
- [ ] T057 [P] Handle unknown enum values in `backend/src/main/java/com/ticketmanagement/config/ApiErrorHandler.java` so the `message` lists allowed values and does not contain a stack trace, SQL, or a Java class name
- [X] T058 Confirm create, edit, and comment forms in `frontend/src/app/tickets/new/page.tsx` and `frontend/src/app/tickets/[id]/page.tsx` keep already typed values when the API returns 400
- [ ] T059 Run the API, restart, and UI checks in `specs/001-support-ticket-management/quickstart.md` and fix any gap those checks expose

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories then proceed in priority order (P1 → P2)
  - US2, US3, and US4 also need a ticket from US1 for their user-facing checks
- **Polish (Phase 8)**: Depends on the user stories you intend to ship

### User Story Dependencies

- **User Story 1 (P1)**: Starts after Foundational. No dependency on other stories
- **User Story 2 (P1)**: Starts after US1 for the HTTP path (needs a stored ticket). `TicketStatusMachineTest` does not need the controller
- **User Story 3 (P2)**: Starts after US1. Does not need US2 except when the independent test uses a Closed ticket; close that ticket through US2 or a test fixture
- **User Story 4 (P2)**: Starts after US1. Does not need US2 or US3 except when the independent test uses a Cancelled ticket
- **User Story 5 (P2)**: Starts after US4, because the independent test searches comment text. Title and description matching also use the US1 ticket fields

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Entities and DTOs before services
- Services before controller methods
- API behavior before the screen that calls it, unless the screen task is marked [P] and only consumes `frontend/src/lib/api.ts`

### Parallel Opportunities

- T002, T003, and T004 can run together after T001
- T006, T007, T008, T009, T010, T011, and T012 can run together after T005
- Once US1 is done, US2 and US3 can proceed in parallel (different rules, coordinate on `TicketService.java` and `TicketController.java`)
- US4 can proceed in parallel with US3 after US1, with the same shared-file caution
- Do not edit `TicketController.java` or `TicketService.java` on two stories at the same time

---

## Parallel Example: User Story 1

```bash
# Tests together:
Task: "T014 TicketServiceTest create/list/get in backend/src/test/java/com/ticketmanagement/service/TicketServiceTest.java"
Task: "T015 TicketControllerIntegrationTest POST and GET in backend/src/test/java/com/ticketmanagement/controller/TicketControllerIntegrationTest.java"

# Models and DTOs together:
Task: "T016 Ticket entity in backend/src/main/java/com/ticketmanagement/entity/Ticket.java"
Task: "T017 Create and response records in backend/src/main/java/com/ticketmanagement/dto/"
Task: "T018 TicketNotFoundException in backend/src/main/java/com/ticketmanagement/exception/TicketNotFoundException.java"

# Screens together after the API client exists:
Task: "T023 List page in frontend/src/app/page.tsx"
Task: "T024 Create page in frontend/src/app/tickets/new/page.tsx"
Task: "T025 Detail page in frontend/src/app/tickets/[id]/page.tsx"
```

## Parallel Example: User Story 2

```bash
Task: "T026 TicketStatusMachineTest in backend/src/test/java/com/ticketmanagement/service/TicketStatusMachineTest.java"
Task: "T028 TicketStatusMachine in backend/src/main/java/com/ticketmanagement/service/TicketStatusMachine.java"
Task: "T029 InvalidStatusTransitionException in backend/src/main/java/com/ticketmanagement/exception/InvalidStatusTransitionException.java"
Task: "T030 UpdateStatusRequest in backend/src/main/java/com/ticketmanagement/dto/UpdateStatusRequest.java"
```

## Parallel Example: User Story 3

```bash
Task: "T034 update cases in backend/src/test/java/com/ticketmanagement/service/TicketServiceTest.java"
Task: "T035 PATCH detail cases in backend/src/test/java/com/ticketmanagement/controller/TicketControllerIntegrationTest.java"
Task: "T036 UpdateTicketRequest in backend/src/main/java/com/ticketmanagement/dto/UpdateTicketRequest.java"
Task: "T037 TicketNotEditableException in backend/src/main/java/com/ticketmanagement/exception/TicketNotEditableException.java"
```

## Parallel Example: User Story 4

```bash
Task: "T041 comment cases in backend/src/test/java/com/ticketmanagement/service/TicketServiceTest.java"
Task: "T043 Comment entity in backend/src/main/java/com/ticketmanagement/entity/Comment.java"
Task: "T044 comment DTOs in backend/src/main/java/com/ticketmanagement/dto/CreateCommentRequest.java"
Task: "T045 CommentNotAllowedException in backend/src/main/java/com/ticketmanagement/exception/CommentNotAllowedException.java"
```

## Parallel Example: User Story 5

```bash
Task: "T050 search cases in backend/src/test/java/com/ticketmanagement/service/TicketServiceTest.java"
Task: "T051 query-param cases in backend/src/test/java/com/ticketmanagement/controller/TicketControllerIntegrationTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Create a ticket, list it, open it, restart with `H2_FILE`, and confirm `mvn test` passes
5. Demo the list and detail flow before status workflow

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. User Story 1 → create, list, view, restart (MVP)
3. User Story 2 → allowed status path and rejected transitions
4. User Story 3 → edit details, blocked on Closed and Cancelled
5. User Story 4 → comments oldest first
6. User Story 5 → keyword and status filter
7. Polish → secret scan, enum error text, quickstart.md

### Parallel Team Strategy

1. Team completes Setup + Foundational together
2. After User Story 1:
   - Developer A: User Story 2 (`TicketStatusMachine` and status endpoint)
   - Developer B: User Story 3 (detail update) after agreeing who owns `TicketService.java` and `TicketController.java`
3. User Story 4 follows User Story 1. User Story 5 follows User Story 4

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps the task to a user story for traceability
- Each user story is independently testable once the stories it depends on are in place
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate that story on its own
- Avoid two people editing `TicketService.java` or `TicketController.java` in the same pass
