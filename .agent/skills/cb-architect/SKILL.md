---
name: cb-architect
description: Governance and architectural standards for the Cloudbreak project. Use this skill to understand directory structures, coding guidelines, commit message rules, and API design principles.
---

# Cloudbreak Architect (Governance)

This skill encodes the foundational mandates and architectural standards for the Cloudbreak project.

## Project Structure & Context
- **Core Services**: `core`, `datalake`, `environment`, `freeipa`, `redbeams`, `autoscale`.
- **Infrastructure**: `cloud-api`, `cloud-aws-*`, `cloud-azure`, `cloud-gcp`.
- **Common Libraries**: `common`, `common-model`, `service-common`, `grpc-common`.
- **CI/CD**: GitHub Actions located in `.github/`.
- **Configuration**: Stored in `application.yml` files (e.g., `core/src/main/resources/application.yml`).

## Coding Guidelines (Mandatory)

### API Design
- **Backwards Compatibility**: Use `String` instead of `Enum` on the API layer for better compatibility.
- **Jakarta Namespace**: Always use `jakarta.*` instead of `javax.*` (Project is on Spring Boot 3).
- **Primitives**: Prefer `boolean` over `Boolean`. If `Boolean` is used, provide a default value.
- **Collections**: Default value must be an empty mutable collection. Mark as required using `@Schema(requiredMode = Schema.RequiredMode.REQUIRED)`.
- **Request Body**: Mark with `@RequestBody(required = true)` for `POST`, `PUT`, `DELETE`.

### Technical Standards
- **Java Version**: 21.
- **Build System**: Gradle.
- **Persistence**: MyBatis Migrations for schema changes (`cbd migrate ...`).
- **Templating**: SaltStack (orchestrator-api, orchestrator-salt) using Jinja.

### Style & Formatting
- **Import Order**:
    1. Static all other imports
    2. (blank line)
    3. `java.*`
    4. (blank line)
    5. `javax.*`
    6. (blank line)
    7. `jakarta.*`
    8. (blank line)
    9. `org.*`
    10. (blank line)
    11. `com.*`
    12. (blank line)
    13. All other imports

## Commit Message Rules
- **Subject Line**: Must begin with ticket number (e.g., `CB-12345`). Capitalize the subject after the ticket number.
- **Subject Length**: Max 72 characters. Do not end with a period.
- **Body**: Separate from subject with a blank line. Explain **what** and **why**, not how. Wrap at 72 characters.
- **Imperative Mood**: Use imperative mood in the subject line (e.g., "Add feature" instead of "Added feature").

## Database Development
- Schema changes must use SQL scripts managed by MyBatis Migrations.
- Locations: `[service]/src/main/resources/schema`.
- Command: `cbd migrate [db] new "Ticket-ID description"`.

## Autonomous & Human-Assisted PRs (cb-junior-dev)

When resolving Jira tickets autonomously or with human assistance using the `cb-junior-dev` persona, the following additional standards apply:

### 1. Verification Section
Every PR must include a "Verification Steps" section in the description, listing:
-   The exact reproduction test case created.
-   The Gradle command used to verify the fix (`./gradlew :module:test --tests ...`).
-   Confirmation of the full module test suite status.

### 2. Impact Analysis
PRs should briefly address the impact on:
-   **API Compatibility**: Confirmation that no breaking changes were introduced to the public API (use of `String` over `Enum`).
-   **Cloud Providers**: List of providers affected (AWS, Azure, GCP, etc.).
-   **Flow State**: If the change involves a `Flow`, confirm that the state machine transitions were audited for leaks or deadlocks.

### 3. Reviewer Selection
Assign reviewers based on the `CODEOWNERS` file in the project root. Identify the target domain using the **Team field (`customfield_10001`)** (e.g., `Raven Clan`). For JQL against Team or resolving Atlassian team ids, see **cb-jira** (`.agent/skills/cb-jira/SKILL.md`). If the change spans multiple domains (e.g., Core and Datalake), ensure at least one reviewer from each domain is requested.
