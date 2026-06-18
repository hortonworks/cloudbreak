---
name: cb-jira
description: Jira Cloud conventions for Cloudbreak — project keys, Team field (customfield_10001), JQL with Atlassian team ids, mascot clan → module mapping, quarterly epic labels.
---

# Cloudbreak Jira playbook

Use this playbook when answering Jira questions, writing JQL, or using Atlassian MCP/API against **CB**. Do not assume it is loaded for generic coding tasks; open **`.agent/skills/cb-jira/SKILL.md`** when Jira context is needed.

## Project

- Default project keys: **`CB`** or **`CLOUDBREAK`** (prefer **`CB`** in JQL unless you specifically need the other).

## Team field (native Jira Cloud Teams)

- **Custom field id**: `customfield_10001`
- **JQL clauses**: `Team` or `cf[10001]`
- **Display names**: **`{MASCOT} Clan`** (e.g. `Raven Clan`, `Wolf Clan`, `Bear Clan`).

### JQL: use Atlassian team id, not display name

Clauses such as `Team = "Raven Clan"` typically return **no rows**. Filter with the **Atlassian team id** from issue JSON, the Jira UI, or your integration.

**Example (Raven Clan):**

```jql
project = CB AND Team = 575d3206-8e71-4f04-9e7e-18bb73a1a118-283 ORDER BY updated DESC
```

Resolve other clans’ ids from `customfield_10001` on a representative issue (REST/MCP) or from Jira, depending on your permissions.

## Domain mapping (Mascot Clan → repository)

Same mapping as **`.agent/WORKFLOW.md`**:

| Clan        | Primary areas |
|-------------|----------------|
| Raven Clan  | `core/`, `freeipa/` |
| Wolf Clan   | `datalake/`, `environment/` |
| Duck Clan   | `redbeams/` |
| Bear Clan   | `autoscale/` |
| Hawk Clan   | `cloud-aws-*`, `cloud-azure/`, `cloud-gcp/` |

## Labels and epics

- **Quarterly epics**: Labels such as `Q4FY26-CB`.

## Confluence (CB wiki scope)

CB engineering docs live under the **Cloudbreak Engineering Home** hub (page id **`92898027`**, space **`ENG`**). Scope searches to that subtree with the `ancestor` filter rather than the whole space, e.g.:

```cql
type = page AND space = "ENG" AND ancestor = 92898027 AND text ~ "your keyword"
```

Pull keywords from the Jira summary, error text, labels, or components. When search returns a page URL, fetch by the numeric **page id** (the segment in `/wiki/spaces/.../pages/<pageId>/...`) to read the full body. If the ticket links outside this subtree, still fetch those pages — the ancestor filter is only the default discovery scope.

## Commits and PRs

- Commit messages use **`CB-XXXXX Subject`** so CI can link PRs to Jira (see root **`AGENTS.md`** — Operational mandates and Continuous integration).
