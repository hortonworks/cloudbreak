---
name: cb-reviewer
description: Standardized PR reviews for Cloudbreak. Use this skill to audit PRs for commit message rules, API design guidelines (Boolean vs boolean, String vs Enum), and rebase compliance.
---

# Cloudbreak Reviewer (Auditor)

This skill provides automated checks for PRs to ensure they follow project standards.

## Auditor Workflows

### 1. Commit Message Validation
Check the PR's commit messages for:
- Subject line starts with `CB-XXXXX`.
- Subject line under 72 chars.
- No trailing period in subject.
- Subject capitalized after the ticket number.
- Imperative mood in subject.
- Blank line between subject and body.
- Body wrapped at 72 chars.

### 2. API Design Audit
Verify that the PR follows API mandates:
- No new `Enum` usage in public APIs; use `String` instead.
- Use `boolean` over `Boolean` unless nullable is required.
- All new API fields are correctly marked required or have default values.
- New endpoints have `@RequestBody(required = true)`.

### 3. Git Compliance
- Check if the PR is a clean rebase on the target branch (look for merge commits).
- Verify that only related files are changed.

### 4. Labeling
Suggest `agent-fix` labels for PRs that were primarily implemented or fixed by an agent.

## Review Criteria
Review should follow the strict guidelines in **cb-architect** (see `.agent/skills/cb-architect/SKILL.md`). If any standard is violated, provide a polite but firm request to correct the violation.
- **Fail PR** if:
    - Wrong commit message format.
    - `Enum` used in public API.
    - Merge commits instead of rebase.
- **Nitpick** if:
    - Incorrect import order.
    - Missing `@Schema` annotations.
