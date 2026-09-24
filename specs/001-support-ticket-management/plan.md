# Implementation Plan: Support Ticket Management

**Branch**: `001-support-ticket-management` | **Date**: 2026-09-20 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-support-ticket-management/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Support staff record, find, update, comment on, and move tickets through a fixed status path. Invalid status changes and invalid input are rejected with a clear message, and tickets survive an application restart.

The backend is Java 21 and Spring Boot 3.5 with Spring Data JPA. Local development uses a file-based H2 database so data survives restart; production uses a `postgres` profile. Controllers under `/api/v1` accept and return DTOs only. A `TicketStatusMachine` in the service layer is the only place that allows or rejects a status change. The frontend is Next.js 16 with React and TypeScript. It calls the REST API and shows the standard error message without clearing the form.

## Technical Context

**Language/Version**: Java 21 (backend); TypeScript on Next.js 16.3.5 and React 19 (frontend)

**Primary Dependencies**: Spring Boot 3.5.16, Spring Web, Spring Data JPA, Bean Validation (`spring-boot-starter-validation`), Lombok, Jackson; Next.js 16.3.5, React 19

**Storage**: H2 file database for the default local profile (`MODE=PostgreSQL`); PostgreSQL when `SPRING_PROFILES_ACTIVE=postgres`. Integration tests use in-memory H2, not a second engine.

**Testing**: JUnit 5 and Mockito for unit tests of the status machine and ticket service. Spring MockMvc integration tests on H2 for HTTP contracts and persistence. Frontend checks are the scenarios in [quickstart.md](./quickstart.md).

**Target Platform**: Linux server for the API; modern desktop browsers for the UI

**Project Type**: Web application (REST backend + Next.js frontend)

**Performance Goals**: A person can create a ticket and see it in under 1 minute. Listing or searching 1,000 tickets returns in under 1 second at the API so the on-screen search goal (under 10 seconds) holds. Status-rule checks add no database round trip of their own.

**Constraints**: Status rules live only in the service-layer state machine. Error JSON has exactly `timestamp`, `status`, `error`, `message`, and `path`. No secrets in the repo; datasource settings come from environment variables. No sign-in in this feature. Last successful save wins; edits are not merged.

**Scale/Scope**: One shared workspace, about 1,000 tickets, tens of people using it at once. Five screens: list, create, detail, edit details, and comment plus status actions on the detail screen.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Requirement | Pre-design result |
|------|-------------|-------------------|
| I. Fixed stack | Java 21, Spring Boot, React on Next.js | Pass. Planning input allowed "React or Next.js"; the constitution requires Next.js, so the frontend is Next.js. |
| II. Layered architecture | controller, service, repository, dto, entity | Pass. `TicketStatusMachine` is a class inside the service layer. Controllers do not call repositories or decide transitions. |
| III. DTO boundary | No entities in controller signatures or JSON | Pass. Records in `dto` are the request and response types. Mapping stays in a service-layer mapper. |
| IV. Test discipline | JUnit 5, Mockito, H2 or Testcontainers | Pass. Unit tests mock repositories. Integration tests run on H2. Testcontainers is not added in this feature because H2 covers the HTTP and persistence gates. |
| V. Error shape | `timestamp`, `status`, `error`, `message`, `path` only | Pass. Field details are folded into `message`. No extra properties, stack traces, SQL, or class names. |
| Security | Env vars only; no committed secrets | Pass. PostgreSQL password and URLs are placeholders. H2 file path is an env var. No `.env` with real values. |
| Quality gates | Tests for logic and for HTTP or persistence changes | Pass. Status machine, service rules, and MockMvc contracts are in scope for implementation. |

Shared enums and the exception handler are not extra behavioral layers. Enums are the status and priority vocabulary used by both entities and DTOs. The handler only translates failures into the error shape. Neither one skips the five layers.

### Post-design re-check

Re-checked after [research.md](./research.md), [data-model.md](./data-model.md), [contracts/openapi.yaml](./contracts/openapi.yaml), and [quickstart.md](./quickstart.md).

All gates still pass. The contract exposes DTOs only, the status path matches the spec and is owned by the state machine, and the error schema has the five required fields and `additionalProperties: false`. No gate exception was required.

## Project Structure

### Documentation (this feature)

```text
specs/001-support-ticket-management/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
backend/
├── pom.xml
└── src/
    ├── main/java/com/ticketmanagement/
    │   ├── TicketManagementApplication.java
    │   ├── controller/TicketController.java
    │   ├── service/TicketService.java
    │   ├── service/TicketStatusMachine.java
    │   ├── service/TicketMapper.java
    │   ├── repository/TicketRepository.java
    │   ├── repository/CommentRepository.java
    │   ├── dto/
    │   ├── entity/Ticket.java
    │   ├── entity/Comment.java
    │   ├── domain/TicketStatus.java
    │   ├── domain/TicketPriority.java
    │   ├── exception/
    │   └── config/ApiErrorHandler.java
    ├── main/resources/application.yml
    ├── main/resources/application-postgres.yml
    └── test/java/com/ticketmanagement/
        ├── service/TicketStatusMachineTest.java
        ├── service/TicketServiceTest.java
        └── controller/TicketControllerIntegrationTest.java

frontend/
├── package.json
├── next.config.ts
└── src/
    ├── app/page.tsx
    ├── app/tickets/new/page.tsx
    ├── app/tickets/[id]/page.tsx
    ├── components/
    └── lib/api.ts
```

**Structure Decision**: Split web app. `backend/` is the Spring Boot API. `frontend/` is the Next.js UI. The browser calls `/api/v1` on the Next.js origin, and Next.js proxies to the API, so the API does not open a permissive cross-origin policy.

## Complexity Tracking

No constitution violations require justification. The status machine is part of the service layer, which is what the planning input required and what Principle II allows.
