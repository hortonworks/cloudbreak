# OpenAPI breaking-change allowlist

Deliberate, reviewed API breaks are suppressed by per-service allowlist files so the
compatibility gate (`integration-test/scripts/openapi-check.sh`, tufin/oasdiff) does not
force you to seed the change into older release lines to make the baseline "compatible".

Each allowlist file lives in the service module that owns the API, at
`<module>/openapi-breaking-allowlist.txt`. The service-to-module mapping is defined in
`integration-test/scripts/service-registry.sh` (the single source of truth for all
service metadata).

## Why this exists

The gate diffs each branch's OpenAPI spec against the latest published build of one or
more baseline lines:

- **on master** — the previous minor line's latest build only.
- **on a release line `X.Y.0`** — its own line's latest build **and** the previous
  minor line (`X.(Y-1).0`) latest build.

Without an allowlist, the only way to land an intentional break was to merge it into an
older (near-prod) branch first and forward-merge up — which can ship the break to prod
via a hotfix **before** it reaches dev/master. That inversion is the risk this mechanism
removes: an allowlist entry is committed on the branch where the break should first ship.

## How to use

1. **Prefer not to break.** Add the new field/endpoint, mark the old one
   `deprecated: true`, and remove it only after the deprecation window. That never
   trips the gate and needs no entry here.
2. **For a real removal/incompatible change**, add one line per break to the
   `openapi-breaking-allowlist.txt` in the corresponding service module (see table above).
3. Each line must be the **full oasdiff breaking-change message** for that change
   (e.g. `in API GET /v4/.../{crn} removed the request property 'foo'`). Get the exact
   text from the failing CI log ("COMPATIBILITY BREAKS in <service>" block) or run
   oasdiff locally. Each line must be the complete message including the
   `in API <METHOD> <path> ...` prefix; a partial line matches nothing. Blank lines are
   skipped. A `#` line is inert only because it holds no full message — ⚠ never paste a
   full breaking-change message into a `#` comment, or it will suppress that change.

## Example

```
in API GET /v4/{workspaceId}/stacks/{name}/foo the 'bar' request parameter 'fooBar' became required
```

## When an entry can be removed

Removability is **per-branch**, because the baseline differs per branch. An entry stops
being needed once *that branch's* baseline build already contains the changed shape — then
oasdiff finds no break even without the entry. Consequences:

- On the **oldest line the break was introduced on**, the previous-minor baseline never
  receives the change (forward-merge only goes up), so that entry is effectively
  **permanent**.
- On **master**, the baseline is the previous minor line. A break introduced on master at
  minor N is never seen by line N-1 (forward-merge only goes up), so the entry is required
  for the whole N cycle. It becomes stale only after the minor roll: once line N is branched
  from master (inheriting the break) and master moves to N+1, master's baseline becomes N —
  which now carries the break — so a clean diff makes the entry droppable.

The `openapi-allowlist-cleanup` GitHub workflow checks this daily **for master only** and
opens a PR removing stale entries.