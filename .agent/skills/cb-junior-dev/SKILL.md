---
name: cb-junior-dev
description: Senior-level thoroughness for Jira ticket resolution in the Cloudbreak project. Use this skill to fetch a Jira ticket, analyze the root cause, create reproduction tests, implement fixes, and prepare a high-quality PR.
---

# Cloudbreak Junior Dev (Senior-Level Thoroughness)

Despite the name, this skill provides **Senior-Level Thoroughness** for the end-to-end development lifecycle. It handles the Jira-to-PR loop with extreme attention to detail, architectural integrity, and testing rigor.

## Core Principles

### 1. Senior Thoroughness
- **Impact Analysis**: Every fix must consider edge cases, backward compatibility (especially at the API layer), and potential side effects in distributed components (e.g., Core vs. Datalake).
- **Clean Code**: Adheres to the module-specific `AGENTS.md` mandates and established patterns. No "just-in-case" logic; only surgical, high-quality changes.
- **Performance**: Considers database query efficiency (MyBatis), thread safety (Virtual Threads), and memory usage.

### 2. Test-Obsessed (Reproduction First)
- **Mandatory Step**: You MUST create a reproduction test case (unit or integration) that explicitly proves the issue before any source code changes are made. **A unit test is a MUST for every change.**
- **Testing Stack**: Use JUnit 5, Mockito, and AssertJ. See [Testing Standards](references/testing_standards.md) for detailed patterns and examples.
- **Verification**: The fix is only complete when the reproduction test passes AND the full module test suite (`./gradlew :<module>:test`) is green.

### 3. Architectural Integrity
- Strictly follows `cb-architect` guidelines (Boolean vs boolean, Jakarta vs Javax, String vs Enum).
- Respects the "Flow" and "State Machine" patterns common in the `core`, `datalake`, and `freeipa` modules.

## Orchestration Workflow

### Phase 1: Research & Discovery
- **Fetch Jira**: Load the issue by whatever integration you have (e.g. Atlassian/Jira MCP, REST API, or text the user pastes from Jira).
- **Log analysis**: If logs or stack traces are attached, read them (download via Jira integration or use content the user provides).
- **Domain Mapping**: Use the **Team field (`customfield_10001`)** to identify target modules based on the Mascot Clan (e.g., `Raven Clan` -> `core/`). JQL, team ids, and full clan table: **cb-jira** (`.agent/skills/cb-jira/SKILL.md`).

### Phase 2: Reproduction & Fix
- **Create Test**: Implement a failing test in the relevant module's `src/test/java` using established patterns (JUnit 5, Mockito, AssertJ).
- **Implement Fix**: Apply the surgical fix in `src/main/java`.
- **Validate**: Run `./gradlew :<module>:test` to verify.

### Phase 3: Cleanup & PR
- **Audit**: Apply the **cb-developer** playbook (`.agent/skills/cb-developer/SKILL.md`) for import standardization and API audits.
- **Commit**: Prepare a commit message using the `CB-XXXXX Subject` format.
- **PR**: Create a PR with a detailed "Technical Rationale" and "Verification Steps" section.

## "Rubber Duck" Protocol
If the fix requires changes in more than 3 modules or involves complex Flow state changes, stop and ask the user for a "Rubber Duck" session to validate the architectural direction.
