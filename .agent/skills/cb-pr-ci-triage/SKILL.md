---
name: cb-pr-ci-triage
description: Triage Cloudbreak pull request GitHub Actions failures from a PR number — unit tests, Jacoco coverage, checkstyle, SpotBugs, integration tests, and related PR checks. Use when a PR is red, CI failed, checks failed, or the user gives a PR number and asks why the build broke.
---

# Cloudbreak PR CI triage

Investigate **failed PR checks** for `cloudbreak/cloudbreak` on **GitHub Enterprise** (`github.infra.cloudera.com`). Workflow: **`.github/workflows/pull-request.yaml`** (`cloudbreak-pull-request-test`).

**Input:** PR number (e.g. `12345`). Optional: failed check name.

**GitHub data:** use **`gh` only** — [references/gh-cli.md](references/gh-cli.md). Do not use GitHub MCP.

**Jobs, failure signals, local repro:** [references/checks-catalog.md](references/checks-catalog.md).

Load **cb-testing** when reproducing unit-test or coverage failures locally.

## Workflow

```
PR CI triage:
- [ ] 1. PR metadata — `gh pr view` (branch, title, Jira from commits)
- [ ] 2. Failed checks — `gh pr checks`
- [ ] 3. Failed logs — `gh run list` → `gh run view --log-failed`
- [ ] 4. Classify job → see checks-catalog.md
- [ ] 5. First root cause only (not cascaded failures)
- [ ] 6. Local repro from checks-catalog.md
- [ ] 7. Report using output template below
```

**Rules**

1. **First failure wins** — first `FAILED` / `BUILD FAILED` / `There were failing tests` in the log.
2. **Ignore green jobs** unless the user asked for a full audit.
3. **Integration logs are huge** — use `--log-failed --job <id>` and `rg` (see gh-cli.md); report failing test class/method.
4. **Flaky vs real** — intermittent aws/fedramp-only failures may be infra; still extract the assertion/error.
5. **Do not guess** — re-fetch truncated logs or open the run URL from `gh run view`.

## Output template

```markdown
## PR #<number> CI triage — <title>

**Branch:** `<head>` → `<base>`
**Run:** <workflow run URL>
**Jira:** CB-XXXXX (from commits, if present)

### Failed checks
| Check | Failing test / root cause | Category |
|-------|---------------------------|----------|
| Integration Test | `FooIT.testBar` — AssertionError at FooIT.java:42 | integration |

### Details
<First error excerpt with file:line>

### Local repro
\`\`\`bash
<minimal command from checks-catalog.md>
\`\`\`

### Recommended fix
<Concrete next step>

### Notes
<Flaky infra, unrelated failures, checks still running>
```

## Related

- [references/gh-cli.md](references/gh-cli.md) — `gh` setup, checks, logs, checkout
- [references/checks-catalog.md](references/checks-catalog.md) — jobs, scoping, repro by failure type
- `.github/workflows/pull-request.yaml`
- `.agent/skills/cb-testing/SKILL.md`
- `.agent/skills/cb-code-reviewer/SKILL.md`
