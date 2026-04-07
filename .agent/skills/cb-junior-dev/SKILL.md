---
name: cb-junior-dev
description: Senior-level thoroughness for Jira ticket resolution in the Cloudbreak project. Use this skill to fetch a Jira ticket, research related Confluence documentation, analyze the root cause, create reproduction tests, implement fixes, and prepare a high-quality PR.
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
- **Fetch Jira**: Load the issue by whatever integration you have (e.g. Atlassian/Jira MCP such as `jira_get_issue` / `mcp-atlassian_jira_get_issue` depending on your client, REST API, or text the user pastes from Jira).
- **Log analysis**: If logs or stack traces are attached, read them (download via Jira integration such as attachment download tools your MCP exposes, or use content the user provides).
- **Confluence** (when you have Jira/Confluence MCP; exact tool names depend on the client, e.g. `mcp-atlassian_confluence_search`, `mcp-atlassian_confluence_get_page`):
    1. **Search**: Scope CB wiki content to the **Cloudbreak Engineering Home** tree — hub page ID **`92898027`**, space **`ENG`**: [Cloudbreak Engineering Home](https://cloudera.atlassian.net/wiki/spaces/ENG/pages/92898027/Cloudbreak+Engineering+Home). Use **CQL** with **`ancestor = 92898027`** so results are only pages under that hub (not the whole ENG space), e.g. `type = page AND space = "ENG" AND ancestor = 92898027 AND text ~ "your keyword"`. Substitute keywords from the Jira **summary**, error text, **labels**, **components**, or feature name. For a first pass, you may fetch the hub page **`92898027`** and follow linked subpages. If the ticket explicitly points elsewhere in Confluence, still fetch those pages — the ancestor filter is the default scope for CB-internal discovery.
    2. **Get page**: When the ticket or search results give a Confluence URL, fetch the page by **page ID** (the numeric segment in `/wiki/spaces/.../pages/<pageId>/...`) to read the full body, not only the snippet from search.
    - **What to pull in**: Pages **linked from the Jira** (description, comments, remote-issue links), plus **runbooks**, **design docs**, ADRs, and onboarding/training pages tied to the same epic or subsystem.
    - Prefer Confluence when the ticket is thin on context, points at a wiki URL, or involves cross-team behavior you need to verify against written standards.
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