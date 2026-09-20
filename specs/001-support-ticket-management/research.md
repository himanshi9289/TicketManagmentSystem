# Research: Support Ticket Management

Decisions for `specs/001-support-ticket-management`. No Technical Context item is left unresolved.

## Spring Boot 3.5.16 on Java 21

- **Decision**: Spring Boot 3.5.16, Java 21, starters for web, data-jpa, and validation. Lombok comes from the Spring Boot BOM.
- **Rationale**: The planning input locks Spring Boot 3.x. 3.5.16 is the latest 3.5 release and runs on Java 21. Boot 3.5 open-source support ended on 2026-06-30; this feature still stays on 3.x because that was an explicit constraint.
- **Alternatives considered**: Spring Boot 4.1 is the supported line as of 2026-09-20. Rejected for this feature because the planning input says 3.x. Moving to 4.x needs a new planning decision, not a silent upgrade.

## Next.js, not a standalone React app

- **Decision**: Next.js 16.3.5 with React 19 and TypeScript. App Router pages for the list, create, and ticket detail. A later 16.3 patch is acceptable.
- **Rationale**: The constitution requires React on Next.js. The planning input said "React (or Next.js)"; the constitution closes that choice. Next.js also proxies `/api/v1` to the backend so the browser stays same-origin.
- **Alternatives considered**: Vite plus React only. Rejected because it drops Next.js. A separate CORS allow-list was rejected in favor of the proxy, which avoids a permissive cross-origin setup.

## File-based H2 locally, PostgreSQL in production

- **Decision**: Default profile uses H2 in file mode with `MODE=PostgreSQL`. Data directory is `H2_FILE` from the environment (a path such as `./data/ticketdb`). Profile `postgres` uses `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` from the environment. `spring.jpa.hibernate.ddl-auto` is `DB_DDL_AUTO` from the environment; local and first production deploy use `update`.
- **Rationale**: The spec requires data to survive a restart. In-memory H2 would fail that check. PostgreSQL mode keeps local SQL closer to production. Credentials are not committed. Schema migration tooling is deferred so this feature does not add a dependency before the model is stable.
- **Alternatives considered**: In-memory H2 (fails restart). Flyway or Liquibase (extra dependency, not needed for two tables). Always-on PostgreSQL for local dev (heavier than the requested H2 profile).

## Status machine is a service-layer class

- **Decision**: `TicketStatusMachine` holds the five allowed steps and is called only by `TicketService`. The same status is a no-op, not an error. Any other pair throws a domain exception before the repository saves. The controller only forwards the requested status.
- **Rationale**: The planning input forbids transition rules in the controller. A small class is unit-testable with JUnit 5 and does not pull in a workflow framework.
- **Alternatives considered**: Spring Statemachine. Rejected as an unnecessary dependency for five edges. Rules inside the controller. Rejected by the planning input and by Principle II.

Allowed steps, and no others:

- `OPEN` → `IN_PROGRESS`
- `OPEN` → `CANCELLED`
- `IN_PROGRESS` → `RESOLVED`
- `IN_PROGRESS` → `CANCELLED`
- `RESOLVED` → `CLOSED`

`CLOSED`, `RESOLVED`, and `CANCELLED` cannot move to `OPEN`. `CLOSED` and `CANCELLED` also reject detail edits and new comments. Detail edits are allowed in `OPEN`, `IN_PROGRESS`, and `RESOLVED`, and they do not call the state machine.

## Error body stays on the five constitution fields

- **Decision**: `ApiErrorHandler` maps validation failures, unknown enums, missing tickets, illegal edits, illegal comments, and illegal transitions onto one JSON object: `timestamp`, `status`, `error`, `message`, `path`. Validation problems are joined into `message` as `field: reason` segments separated by `; `.
- **Rationale**: Principle V forbids extra fields. The spec still needs each bad field named. The UI keeps form state locally and shows `message`, so values are not dropped on a 400.
- **Alternatives considered**: A `fields` array on the error body. Rejected because it breaks the constitution. Different shapes per endpoint. Rejected for the same reason.

## DTOs are records; entities use Lombok carefully

- **Decision**: Request and response types are Java records with Bean Validation annotations. JPA entities use Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`, and `@Builder`. They do not use `@Data`.
- **Rationale**: Records match an immutable API contract and work with Bean Validation on Spring Boot 3.5. Lombok on entities supplies the no-arg constructor JPA needs. `@Data` would generate `equals` and `hashCode` that are unsafe on JPA entities with generated ids and lazy comments.
- **Alternatives considered**: Lombok on DTOs as well. Rejected because records are enough and stay immutable. Entities returned from controllers. Rejected by Principle III.

## No authentication

- **Decision**: No login, tokens, or roles. Every caller can perform every allowed operation. Assignee is an optional name string, not a user id.
- **Rationale**: Spec FR-015 and the assumptions say sign-in is out of scope.
- **Alternatives considered**: Session login or OAuth. Rejected as scope expansion.

## List returns every match, unordered by the client

- **Decision**: `GET /api/v1/tickets` returns the full match set as an array, ordered by `updatedAt` descending. No page parameters in this feature. Keyword length is capped at 200 characters. `%` and `_` in the keyword are matched literally.
- **Rationale**: The spec says clearing filters shows every ticket, and the scale target is 1,000 rows. Paging would hide tickets the spec says must be listed. The keyword cap and wildcard escaping keep the search predictable.
- **Alternatives considered**: Offset paging. Rejected for v1 because an empty page can look like "no tickets" and the spec wants the full list. A search index. Unnecessary at 1,000 rows.

## Tests stay on H2

- **Decision**: Status-machine tests are plain JUnit 5. Service tests use Mockito for repositories. Controller tests use MockMvc, `@SpringBootTest`, and in-memory H2. Production PostgreSQL is not started in the automated suite.
- **Rationale**: Principle IV requires JUnit 5, Mockito, and H2 or Testcontainers. H2 satisfies the persistence and HTTP gate. Adding Testcontainers would be a new dependency without a PostgreSQL-only behavior in this feature. H2's PostgreSQL compatibility mode covers the case-insensitive `LIKE` used for search.
- **Alternatives considered**: Testcontainers PostgreSQL for every build. Rejected until a bug is specific to PostgreSQL. A second embedded database. Forbidden by Principle IV.

## Last write wins

- **Decision**: No optimistic lock column. Two overlapping saves: the later committed transaction replaces the earlier one.
- **Rationale**: The spec says the system does not merge concurrent edits.
- **Alternatives considered**: `@Version` and HTTP 409. Rejected because it changes the specified conflict behavior.
