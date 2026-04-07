---
name: cb-triage
description: Turn a Jira issue into an actionable engineering plan for Cloudbreak—without implementing code. Use this skill for fast triage, scope discovery, and verification checklists using Atlassian MCP, Confluence (CB Engineering Home), and local repo inspection.
---

# Cloudbreak Triage (Issue → Plan)

Use this skill when the goal is **understanding and planning**, not shipping a fix. For implementation, follow **`cb-junior-dev`** and `.gemini/WORKFLOW.md`.

## Deliverables (always produce these)

1. **Problem statement** — What is broken or missing, in one short paragraph (ground in Jira + attachments).
2. **Intended behavior** — What “good” looks like; call out ambiguities and questions for the reporter.
3. **Affected surfaces** — Modules, packages, APIs, Flows, or cloud providers; map **Team / Mascot Clan** (`customfield_10001`) to directories per **`cb-architect`** and root `GEMINI.md`.
4. **Standards that apply** — Pointers to **`cb-architect`** rules (API strings vs enums, `boolean`, `jakarta.*`, etc.) and any Confluence policy you found.
5. **Likely change points** — Classes or areas to read first (from description, stack traces, and git grep in repo).
6. **Verification story** — Unit tests to add or extend (`./gradlew :<module>:test`), integration expectations if any, rollout or ops notes from wiki.
7. **Open risks** — Backward compatibility, multi-module impact, or need for “Rubber Duck” (`cb-junior-dev` protocol).

## Phase 1: Jira

- **`mcp-atlassian_jira_get_issue(ticket_id)`** — Description, status, links, components, labels, **Team field (`customfield_10001`)**, related epic/version.
- **`mcp-atlassian_jira_download_attachments`** — Logs, screenshots, HARs, thread dumps referenced on the ticket.

## Phase 2: Confluence (CB wiki scope)

Same tools and **default scope** as **`cb-junior-dev`** Phase 1:

- **Hub**: [Cloudbreak Engineering Home](https://cloudera.atlassian.net/wiki/spaces/ENG/pages/92898027/Cloudbreak+Engineering+Home) (`page_id` **92898027**, space **ENG**).
- **`mcp-atlassian_confluence_search`** — Prefer CQL: `type = page AND space = "ENG" AND ancestor = 92898027 AND text ~ "keywords from ticket"`.
- **`mcp-atlassian_confluence_get_page`** — Full page body when you have a page ID or hub entry to expand.
- If Jira links **outside** this subtree, still fetch those pages for triage.

## Phase 3: Repository context (local)

- Read the target module’s **`GEMINI.md`** if it exists.
- **Search** the codebase for error strings, class names, or feature flags from the ticket (`rg`, IDE search).
- **`git log` / blame** (via terminal) on hot paths to find prior fixes and ownership—summarize commit subjects and age, do not guess authorship policy.

## Phase 4: Optional remote PR signal

If **GitHub MCP** is configured and the ticket mentions a PR or branch, use **`cb-github-workflows`** to pull check status and review threads into the plan.

## Handoff

- End with a short **“Next steps”**: bullets for **`cb-junior-dev`** (repro test first) or for human confirmation when scope is unclear.
