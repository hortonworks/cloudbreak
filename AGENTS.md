# Cloudbreak agent instructions (ground truths)

This file defines foundational mandates for automated assistants working in the Cloudbreak repository. These instructions take precedence over default behaviors.

Submodules and packages may also include their own **`AGENTS.md`** files with **module-specific** context; treat those as local addenda when working in those paths.

Some coding-assistant products expect skills in a directory other than **`.agent/skills/`**. Configure yours to use **`.agent/skills/`** (or the path your tool documents for overrides). This repository does not maintain parallel copies of playbooks elsewhere.

## Specialized playbooks

Assistant-oriented playbooks live under **`.agent/skills/`**. The files are **plain Markdown** (each `SKILL.md` may start with optional YAML front matter for tools that index skills). Any assistant that can open Markdown can follow the same instructions by **opening those files**.

- **cb-jira**: Jira project keys, Team field / JQL (Atlassian team ids), clan → module mapping, quarterly labels — load when working with tickets or Jira integrations.
- **cb-code-reviewer**: How the team reviews PRs (distilled from real review history) — reuse-over-duplication, logging, tests, null-safety, flow/layering, correctness bugs. Load when reviewing or self-reviewing a PR.
- **cb-flow-engine**: Step-by-step procedure for adding a flow (state machine config, states/events, actions, handlers, flow chains) without deadlocking the engine — load when implementing a long-running async operation.
- **cb-db-migration**: MyBatis Migrations procedure — file location, timestamp+ticket naming, mandatory `@UNDO`, idempotent/reversible DDL — load when changing a service database schema.
- **cb-testing**: JUnit 5 + Mockito unit tests, Spring integration tests, the authorization-compliance test, flow-chain graph tests, Gradle/jacoco commands and coverage gates — load when writing tests or before opening a PR.
- **cdp-docs**: Official CDP Public Cloud documentation lookup and verification.
- **cb-cloud-providers**: Read-first AWS/Azure/GCP CLI/MCP recipes for inspecting live cloud state.

The end-to-end Jira-to-PR workflow (reproduction-first testing, PR quality bar) lives in **`.agent/WORKFLOW.md`**.

How you load them depends on your tool (for example, enabling a skill in your assistant's UI, `@`-including a skill file in your editor, or pasting an excerpt into a chat session).

## Project foundations

- **Java version**: 21 (Spring Boot 3.3).
- **Build system**: Gradle (`./gradlew clean build`).
- **Database**: MyBatis Migrations (`cbd migrate ...`).
- **Templating**: SaltStack (Jinja) in `orchestrator-api` and `orchestrator-salt`.

## Logical domain mapping

- **Core orchestration**: `core/`, `datalake/`, `environment/`, `freeipa/`, `redbeams/`.
- **Infrastructure**: `cloud-api/`, `cloud-aws-*`, `cloud-azure/`, `cloud-gcp/`.
- **Common logic**: `common/`, `common-model/`, `service-common/`, `grpc-common/`.
- **Public API**: `core-api/`, `datalake-api/`, `environment-api/`, `freeipa-api/`, `redbeams-api/`.
- **Infrastructure templates**:
  - **Blueprints (CM)**: `core/src/main/resources/defaults/blueprints/*.bp`
  - **Cluster templates (cloud)**: `core/src/main/resources/defaults/clustertemplates/*.json`

## Cross-cutting patterns

These patterns are implemented from many modules, but documented in one. When a task involves them, read the source module's `AGENTS.md` even if you are working elsewhere:

- **Flow engine** (state machine, Action/Handler async pattern): `flow/AGENTS.md`. Used by `core/`, `datalake/`, `environment/`, `freeipa/`, `redbeams/`.
- **Quartz scheduled jobs** (`MdcQuartzJob`, `JobInitializer`, `TransactionalScheduler`): `common/AGENTS.md`. Used across Core, Flow, FreeIPA, Datalake, Environment, Redbeams.

## Continuous integration (PR testing)

Pull requests run GitHub Actions (see `.github/workflows/pull-request.yaml`):

- **Unit tests**: All modules on every PR.
- **Jira**: PRs are linked to Jira tickets from the `CB-XXXXX` prefix in commit messages.
- **Static analysis**: Code coverage and integration test coverage checks.

## Operational mandates

1. **Commit messages**: Use `CB-XXXXX Subject` (imperative, under 72 characters, no trailing period).
2. **API changes**: Prefer `String` over `Enum` for backward compatibility. Prefer `boolean` over `Boolean`.
3. **Namespace**: Use `jakarta.*`, not `javax.*`, for persistence, validation, and servlet APIs.
4. **Import order**: static imports, then `java.*`, `javax.*`, `jakarta.*`, `org.*`, `com.*`, then all other imports — each group separated by a blank line.
5. **Configuration**: Use `application.yml` for service configuration.
6. **Documentation**: See `README-devnotes.md` for environment setup and `README.md` for full coding guidelines. To profile a running service JVM with JProfiler in Kubernetes (and the FIPS "Attach" workaround), see `docs/profiling-jprofiler-on-kubernetes.md`.

## External resources

- **Wiki — Engineering home**: [Cloudbreak Engineering Home](https://cloudera.atlassian.net/wiki/spaces/ENG/pages/92898027/Cloudbreak+Engineering+Home)
- **Wiki — Training**: [TRAINING materials](https://cloudera.atlassian.net/wiki/spaces/ENG/pages/697991211/TRAINING+materials)
- **Wiki — Runbooks**: [Developer Guides and Runbooks](https://cloudera.atlassian.net/wiki/spaces/ENG/pages/1033961582/Developer+Guides+and+Runbooks)
- **Jira**: Project keys, Team field, JQL, and clan mapping — **cb-jira** playbook (`.agent/skills/cb-jira/SKILL.md`); load when needed, not for every task.
