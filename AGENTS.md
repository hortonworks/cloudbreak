# Cloudbreak agent instructions (ground truths)

This file defines foundational mandates for automated assistants working in the Cloudbreak repository. These instructions take precedence over default behaviors.

Submodules and packages may also include their own **`AGENTS.md`** files with **module-specific** context; treat those as local addenda when working in those paths.

Some coding-assistant products expect skills in a directory other than **`.agent/skills/`**. Configure yours to use **`.agent/skills/`** (or the path your tool documents for overrides). This repository does not maintain parallel copies of playbooks elsewhere.

## Specialized playbooks

Assistant-oriented playbooks live under **`.agent/skills/`**. The files are **plain Markdown** (each `SKILL.md` may start with optional YAML front matter for tools that index skills). Any assistant that can open Markdown can follow the same instructions by **opening those files**.

- **cb-architect**: Governance, coding guidelines, and commit message standards.
- **cb-developer**: Janitorial tasks (imports, API audits, Jakarta namespace checks).
- **cb-reviewer**: PR auditing and compliance verification.
- **cdp-docs**: Official CDP Public Cloud documentation lookup and verification.
- **cb-junior-dev**: Jira-to-PR workflow, reproduction-first testing, and PR quality bar (see also `.agent/WORKFLOW.md`).
- **cb-jira**: Jira project keys, Team field / JQL (Atlassian team ids), clan → module mapping, quarterly labels — load when working with tickets or Jira integrations.

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

## Continuous integration (PR testing)

Pull requests run GitHub Actions (see `.github/workflows/pull-request.yaml`):

- **Unit tests**: All modules on every PR.
- **Jira**: PRs are linked to Jira tickets from the `CB-XXXXX` prefix in commit messages.
- **Static analysis**: Code coverage and integration test coverage checks.

## Operational mandates

1. **Commit messages**: Use `CB-XXXXX Subject` (imperative, under 72 characters, no trailing period).
2. **API changes**: Prefer `String` over `Enum` for backward compatibility. Prefer `boolean` over `Boolean`.
3. **Namespace**: Use `jakarta.*`, not `javax.*`, for persistence, validation, and servlet APIs.
4. **Configuration**: Use `application.yml` for service configuration.
5. **Documentation**: See `README-devnotes.md` for environment setup and `README.md` for full coding guidelines.

## External resources

- **Wiki — Engineering home**: [Cloudbreak Engineering Home](https://cloudera.atlassian.net/wiki/spaces/ENG/pages/92898027/Cloudbreak+Engineering+Home)
- **Wiki — Training**: [TRAINING materials](https://cloudera.atlassian.net/wiki/spaces/ENG/pages/697991211/TRAINING+materials)
- **Wiki — Runbooks**: [Developer Guides and Runbooks](https://cloudera.atlassian.net/wiki/spaces/ENG/pages/1033961582/Developer+Guides+and+Runbooks)
- **Jira**: Project keys, Team field, JQL, and clan mapping — **cb-jira** playbook (`.agent/skills/cb-jira/SKILL.md`); load when needed, not for every task.
