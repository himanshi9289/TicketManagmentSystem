<!--
Sync Impact Report
- Version change: unratified template → 1.0.0
- Modified principles:
  - [PRINCIPLE_1_NAME] → I. Fixed Technology Stack
  - [PRINCIPLE_2_NAME] → II. Layered Architecture
  - [PRINCIPLE_3_NAME] → III. DTO API Boundary
  - [PRINCIPLE_4_NAME] → IV. Test Discipline
  - [PRINCIPLE_5_NAME] → V. Standard REST Error Responses
- Added sections:
  - Security and Configuration
  - Quality Gates
- Removed sections: none (placeholder scaffold replaced)
- Follow-up TODOs: none
-->

# Support Ticket Management System Constitution

## Core Principles

### I. Fixed Technology Stack

The backend MUST be Java 21 and Spring Boot. The frontend MUST be React on
Next.js. Features MUST be implemented on this stack. Introducing a different
language, framework, or runtime requires a constitution amendment before any
code lands.

Rationale: One stack keeps reviews, tests, and deployment predictable. Drift
into alternate runtimes splits the test and operations model.

### II. Layered Architecture

Backend code MUST be organized into these layers, and only these layers, for
application behavior: controller, service, repository, dto, and entity.

- Controllers MUST handle HTTP only: routing, request binding, status codes,
  and delegation to a service. Controllers MUST NOT contain business rules and
  MUST NOT call repositories.
- Services MUST own business rules and transactions. Services MUST NOT depend
  on controller or HTTP types.
- Repositories MUST be the only persistence access. No other layer MAY issue
  queries or touch the data store directly.
- Entities MUST represent persistence state only. DTOs MUST represent API
  contracts only. The two MUST NOT be collapsed into one type.

Rationale: A fixed layering keeps persistence, business rules, and HTTP
separable and independently testable.

### III. DTO API Boundary

Controllers MUST accept and return DTOs. Raw entities MUST NOT appear in
request bodies, response bodies, or controller method signatures. Mapping
between entity and DTO MUST occur in the service layer or in a mapper invoked
only by the service. Persistence annotations, lazy associations, and internal
identifiers that are not part of the API contract MUST NOT leak through the
boundary.

Rationale: Exposing entities couples clients to the schema and leaks fields
that were never part of the contract.

### IV. Test Discipline

- Unit tests MUST use JUnit 5 and Mockito. Production logic MUST be covered by
  unit tests that mock collaborators outside the unit under test.
- Integration tests MUST use H2, Testcontainers, or both. Other databases or
  embedded engines MUST NOT be introduced for tests without an amendment.
- Every bug fix MUST include a regression test that fails on the unfixed
  behavior and passes after the fix. A bug fix without that test MUST NOT be
  merged.

Rationale: A single test stack and a required regression test stop the same
defect from returning under a different name.

### V. Standard REST Error Responses

Every REST error response MUST use this JSON object and no other shape:

- `timestamp`: when the error was produced (ISO-8601)
- `status`: HTTP status code
- `error`: HTTP reason phrase
- `message`: human-readable explanation safe to show a client
- `path`: request path that failed

Validation failures, unhandled exceptions, and domain errors MUST all use this
object. Stack traces, SQL, and internal class names MUST NOT appear in
`message` or in any extra field. Success responses are not required to use
this shape.

Rationale: One error contract lets the frontend and clients handle failures
without per-endpoint special cases.

## Security and Configuration

Secrets MUST NOT be committed. This includes API keys, tokens, passwords,
certificates, private keys, and OAuth credentials in source, tests, fixtures,
logs, and documentation.

Application configuration values MUST be supplied by environment variables.
Committed files MAY name keys that resolve from the environment. They MUST NOT
contain secret values or environment-specific credentials. Hardcoded secrets
and checked-in `.env` files with real values are prohibited.

Rationale: Environment-only configuration keeps credentials out of history and
out of review diffs.

## Quality Gates

A change MUST NOT merge unless it satisfies every applicable principle above.

- New or changed business logic MUST have JUnit 5 and Mockito unit tests.
- Persistence or HTTP contract changes MUST have an H2 or Testcontainers
  integration test.
- Bug fixes MUST include the regression test required by Principle IV.
- Reviewers MUST reject controllers that return entities, layers that skip the
  stack, error bodies that omit any required field, and any committed secret.
- Complexity beyond the five layers MUST be justified in the change description
  or it MUST be removed.

## Governance

This constitution supersedes conflicting local practice, README notes, and
template examples. If a spec, plan, or task conflicts with these principles,
the constitution wins until it is amended.

Amendments MUST be made by editing `.specify/memory/constitution.md`, recording
the version bump and rationale, and reviewing dependent specs and plans for
compliance. Amendments MUST NOT be applied by silent convention.

Versioning follows semantic versioning:

- MAJOR: a principle is removed or redefined in a backward-incompatible way.
- MINOR: a principle or section is added or materially expanded.
- PATCH: wording is clarified without changing the rule.

Compliance review is part of every pull request. Reviewers MUST check the
change against Core Principles, Security and Configuration, and Quality Gates
before approval. Runtime development guidance is this file.

**Version**: 1.0.0 | **Ratified**: 2026-09-20 | **Last Amended**: 2026-09-20
