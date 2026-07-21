# GitHub CLI reference (PR CI triage)

Use **`gh` only** for PR metadata, checks, workflow runs, and logs. Do not use GitHub MCP — it is unreliable in this environment (Docker spawn issues, Enterprise host config).

**Host:** `github.infra.cloudera.com`  
**Repo:** `cloudbreak/cloudbreak`  
**PR workflow:** `cloudbreak-pull-request-test` (`.github/workflows/pull-request.yaml`)

## Setup

Set enterprise host once per shell (`gh` defaults to github.com otherwise):

```bash
export GH_HOST=github.infra.cloudera.com
export GH_REPO=cloudbreak/cloudbreak   # optional; or pass --repo on each command

PR=12345
```

Verify auth:

```bash
gh auth status   # must list github.infra.cloudera.com
```

If not authenticated:

```bash
gh auth login --hostname github.infra.cloudera.com   # user completes interactively
```

## PR metadata

```bash
# Summary: title, branch, author, state, check rollup
gh pr view "$PR" --repo cloudbreak/cloudbreak \
  --json number,title,url,author,headRefName,baseRefName,state,statusCheckRollup

# Failed / pending checks only
gh pr checks "$PR" --repo cloudbreak/cloudbreak

# Wait for checks to finish
gh pr checks "$PR" --repo cloudbreak/cloudbreak --watch

# Commits (for Jira key CB-XXXXX in messages)
gh pr view "$PR" --repo cloudbreak/cloudbreak --json commits -q '.commits[].messageHeadline'

# Changed files
gh pr view "$PR" --repo cloudbreak/cloudbreak --json files -q '[.files[].path]'

# Full diff (pipe through rg for targeted search)
gh pr diff "$PR" --repo cloudbreak/cloudbreak
```

## Workflow runs and logs

```bash
BRANCH=$(gh pr view "$PR" --repo cloudbreak/cloudbreak --json headRefName -q .headRefName)

# Latest PR workflow runs on the branch
gh run list --repo cloudbreak/cloudbreak \
  --branch "$BRANCH" --workflow cloudbreak-pull-request-test --limit 3

RUN_ID=<id from list>

# Job summary (name, conclusion, job id, run URL)
gh run view "$RUN_ID" --repo cloudbreak/cloudbreak --json conclusion,jobs,url,attempt

# Run still in progress?
gh run view "$RUN_ID" --repo cloudbreak/cloudbreak --json status,conclusion

# Failed jobs only
gh run view "$RUN_ID" --repo cloudbreak/cloudbreak --json jobs \
  -q '.jobs[] | select(.conclusion != "success") | {name, conclusion, id}'

# All failed-step logs (start here)
gh run view "$RUN_ID" --repo cloudbreak/cloudbreak --log-failed 2>&1 \
  | rg -n 'FAILED|error:|Checkstyle|SpotBugs|AssertionError|BUILD FAILED' | head -40

# Single job logs (when run is huge)
gh run view "$RUN_ID" --repo cloudbreak/cloudbreak --log-failed --job <job-id>
```

## Local checkout (for repro)

```bash
gh pr checkout "$PR" --repo cloudbreak/cloudbreak
git fetch origin master
```

## Troubleshooting

| Problem | Command |
|---------|---------|
| Logs too large | `gh run view RUN_ID --repo cloudbreak/cloudbreak --log-failed --job JOB_ID 2>&1 \| rg -n 'FAILED\|error:\|Checkstyle\|SpotBugs\|AssertionError\|BUILD FAILED' \| head -40` |
| Check pending | `gh pr checks PR --repo cloudbreak/cloudbreak --watch` |
| Wrong repo/host | `gh auth status`; set `export GH_HOST=github.infra.cloudera.com` |
| No failed logs yet | `gh run view RUN_ID --repo cloudbreak/cloudbreak --json status,conclusion` |
