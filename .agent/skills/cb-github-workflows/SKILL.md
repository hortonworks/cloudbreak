---
name: cb-github-workflows
description: Use GitHub (via MCP) for Cloudbreak PRs, checks, and diffs from the editor. Pairs with cb-reviewer for standards and cb-triage/cb-junior-dev for ticket context.
---

# Cloudbreak GitHub Workflows (MCP)

This skill describes **how to wire GitHub into Cloudbreak work** when a GitHub MCP server is available. Exact tool names depend on the MCP package and client; they often appear as **`mcp_github_*`** or **`github_*`**. Prefer **read** operations unless the user explicitly asks to create or merge.

## Common tasks → what to fetch

| Goal | Approach |
|------|----------|
| Find PRs for ticket **CB-XXXXX** | Search PRs/commits where title or branch contains `CB-XXXXX`; GitHub Jira linking on PRs is driven by **`CB-XXXXX` in commit subjects** (see root `GEMINI.md` CI section). |
| Understand **CI / checks** | List workflow runs or check suites for the PR’s head SHA; summarize failing jobs and log hints. |
| Review **diff scope** | Get the PR diff or file list; cross-check with **`cb-reviewer`** (only related files, no surprise churn). |
| Read **review comments** | Thread review comments and inline feedback; group by file for actionable edits. |
| Compare to **base branch** | Confirm PR is rebased or merged with target; **`cb-reviewer`** flags merge commits on feature branches. |

## Repo facts (Cloudbreak)

- **Remote**: Organization project **`cloudbreak/cloudbreak`** (see `.github/workflows/pull-request.yaml` `PROJECT` env).
- **PR CI**: Workflows under **`.github/workflows/`** — e.g. `pull-request.yaml` runs **Jira update**, **unit tests**, and other jobs on `pull_request`.
- **Branch naming**: Feature branches often `CB-XXXXX-short-description` per `.gemini/WORKFLOW.md`.

## Suggested tool mapping (illustrative)

Use whatever names your client exposes; typical operations include:

- **Get / list pull requests** for the repo and filter by head branch or label.
- **Get PR files or diff** for summary of touched modules (`core/`, `datalake/`, etc.).
- **List check runs / status** for the latest commit on the PR.
- **Get issue / PR comments** for automation notes or reviewer requests.

If a tool is missing, fall back to **`gh` CLI** in the terminal (read-only: `gh pr view`, `gh pr checks`) where authenticated.

## Pairing with other skills

- **`cb-reviewer`** — Apply commit message, API, and rebase rules to what GitHub returns.
- **`cb-triage` / `cb-junior-dev`** — Correlate Jira state with PR state; paste PR URL into Jira when closing the loop per `.gemini/WORKFLOW.md` Phase 6.

## Safety

- Do not post secrets, tokens, or internal vault paths into public issues or PR bodies.
- Prefer summarizing CI failures rather than dumping full logs unless the user needs them.
