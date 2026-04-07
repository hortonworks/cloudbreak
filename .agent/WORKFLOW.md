# Cloudbreak Development SOP: Jira-to-PR Workflow

This workflow defines the standard operating procedure for resolving Jira tickets using the `cb-junior-dev` persona. It can be executed autonomously or with human intervention.

## Phase 1: Jira Ingestion
1.  **Ticket ID**: User specifies a ticket (e.g., `CB-12345`).
2.  **Fetch issue**: Retrieve description, **Team field (`customfield_10001`)**, and attachments via your Jira integration (e.g. Atlassian MCP, API, or user-provided excerpt). For JQL, team ids, and clan conventions, use **`.agent/skills/cb-jira/SKILL.md`**.
3.  **Module Deep-Dive**: Read the specific `AGENTS.md` in the target module.

## Phase 2: Reproduction (Critical)
1.  **Isolate Failure**: Search the module's `src/test/java` for similar test cases.
2.  **Create Failing Test**: Add a new test case that isolates the bug or demonstrates the missing requirement.
3.  **Validate Failure**: Run `./gradlew :<module>:test --tests <TestName>` and confirm it fails.

## Phase 3: Senior-Level Implementation
1.  **Surgical Fix**: Modify only the code necessary to satisfy the failing test.
2.  **Standards Check**:
    -   Use `boolean` primitives.
    -   Use `jakarta.*` namespace.
    -   Use `String` on APIs for backward compatibility.
3.  **Verify**: Run the reproduction test and confirm it passes.

## Phase 4: Full-Module Validation
1.  **Audit**: Run the `cb-developer` skill for import standardization and API audits.
2.  **Full Test Suite**: Run `./gradlew :<module>:test`.
3.  **Checkstyle/Lint**: (Optional) Run `./gradlew checkstyleMain` if configured.

## Phase 5: Commit & PR
1.  **Branching**: Create a feature branch `CB-XXXXX-short-description`.
2.  **Commit Message**:
    ```text
    CB-XXXXX <Short Imperative Subject>

    <What changed and Why (Senior-level explanation)>
    ```
3.  **PR Body**:
    -   **Summary**: Technical rationale of the fix.
    -   **Verification**: List of tests executed.
4.  **PR creation**: Open the PR using your Git hosting workflow (e.g. GitHub MCP, `gh` CLI, or the web UI).

## Phase 6: Closure
1.  **Jira Update**: Add a comment to the Jira ticket with a link to the PR.
2.  **Status**: Move Jira to "Resolved" or "Awaiting PR Review" (if permitted).
