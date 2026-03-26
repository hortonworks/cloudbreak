# Cloudbreak Gemini Integration (Ground Truths)

This file contains the foundational mandates for Gemini agents in the Cloudbreak project. These instructions take absolute precedence over default behaviors.

## 🤖 Agent Personas
Three specialized personas are available in the `.gemini/skills/` directory. Use `activate_skill` to invoke them:

- **`cb-architect`**: Governance, coding guidelines, and commit message standards.
- **`cb-developer`**: Janitorial tasks (imports, API audits, Jakarta namespace checks).
- **`cb-reviewer`**: PR auditing and compliance verification.
- **`cdp-docs`**: Official CDP Public Cloud documentation lookup and verification.

## 🛠 Project Foundations
- **Java Version**: 21 (Spring Boot 3.3).
- **Build System**: Gradle (`./gradlew clean build`).
- **Database**: MyBatis Migrations (`cbd migrate ...`).
- **Templating**: SaltStack (Jinja) in `orchestrator-api` and `orchestrator-salt`.

## 📂 Logical Domain Mapping
- **Core Orchestration**: `core/`, `datalake/`, `environment/`, `freeipa/`, `redbeams/`.
- **Infrastructure**: `cloud-api/`, `cloud-aws-*`, `cloud-azure/`, `cloud-gcp/`.
- **Common Logic**: `common/`, `common-model/`, `service-common/`, `grpc-common/`.
- **Public API**: `core-api/`, `datalake-api/`, `environment-api/`, `freeipa-api/`, `redbeams-api/`.
- **Infrastructure Templates**:
  - **Blueprints (CM)**: `core/src/main/resources/defaults/blueprints/*.bp`
  - **Cluster Templates (Cloud)**: `core/src/main/resources/defaults/clustertemplates/*.json`

## ✅ Continuous Integration (PR Testing)
All Pull Requests trigger automated workflows via GitHub Actions (defined in `.github/workflows/pull-request.yaml`):
- **Unit Tests**: Executed for all modules on every PR.
- **Jira Update**: Automatically links PRs to their respective Jira tickets based on the `CB-XXXXX` prefix in commit messages.
- **Static Analysis**: Code coverage and integration test coverage checks are automated.

## ⚖ Operational Mandates
1. **Commit Messages**: Must follow the `CB-XXXXX Subject` format (Imperative, <72 chars, no trailing period).
2. **API Changes**: Use `String` instead of `Enum` for backward compatibility. Use `boolean` over `Boolean`.
3. **Namespace**: Always use `jakarta.*` instead of `javax.*` for persistence, validation, and servlet APIs.
4. **Configuration**: Use `application.yml` for service configurations.
5. **Documentation**: Refer to `README-devnotes.md` for environment setup and `README.md` for full coding guidelines.

## 🔗 External Resources
- **Wiki - Engineering Home**: [Cloudbreak Engineering Home](https://cloudera.atlassian.net/wiki/spaces/ENG/pages/92898027/Cloudbreak+Engineering+Home)
- **Wiki - Training**: [TRAINING materials](https://cloudera.atlassian.net/wiki/spaces/ENG/pages/697991211/TRAINING+materials)
- **Wiki - Runbooks**: [Developer Guides and Runbooks](https://cloudera.atlassian.net/wiki/spaces/ENG/pages/1033961582/Developer+Guides+and+Runbooks)
- **Jira**: Default project is `CB` or `CLOUDBREAK`.
- **Jira Teams**: Managed via native Jira Cloud Teams (custom field `customfield_10001`). Teams follow the `{MASCOT} Clan` naming convention (e.g., `Raven Clan`, `Wolf Clan`, `Bear Clan`).
- **Quarterly Epics**: Marked with labels like `Q4FY26-CB`.
