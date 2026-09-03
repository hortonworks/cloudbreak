---
name: cb-swagger-check
description: Run Cloudbreak's OpenAPI/Swagger compatibility gate locally and land a deliberate breaking change via the per-service breaking-change allowlist. Use when a removal/incompatible API change is intentional and needs an allowlist entry, or to diagnose a red "Swagger Compatibility" check. An allowlist entry is only ever correct for an intentional break — never suppress a break you did not mean to make; fix the code instead.
---

# Cloudbreak Swagger compatibility check & breaking-change allowlist

The **Swagger Compatibility** PR check diffs each service's generated OpenAPI spec against
already-published baseline builds with [tufin/oasdiff](https://github.com/tufin/oasdiff).
If the diff contains **breaking** changes that aren't suppressed by an allowlist, the gate
fails and only admins can merge — which blocks *every other* PR too. This skill is how to
reproduce that check locally and, when a break is intentional, allowlist it correctly.

The single source of truth for all service metadata (name, module, S3 zone, spec path, the
oasdiff docker image) is `integration-test/scripts/service-registry.sh`. The mechanism is
documented in `integration-test/openapi-breaking-allowlist/README.md`.

## First: is the break intentional?

A red gate is a signal, **not** a to-do to allowlist. Before touching any allowlist,
identify each `error` row and decide whether *this PR meant to make that change*:

- **Intentional** (you deliberately removed/renamed/tightened an API as part of this
  work) → continue with this skill and allowlist it.
- **Unintentional** (a rename, enum edit, required-param addition, or type change you
  didn't realize was incompatible) → **do not allowlist it.** The gate caught a real
  regression; fix the code (revert the change, keep the old value, make the param
  optional, or use the deprecate-then-remove path in Option 1). Allowlisting here would
  silently ship a break to API consumers.

If any row is unclear, treat it as unintentional and confirm with the change's author
before suppressing. Only add allowlist lines for the specific rows you can affirmatively
say are intended — never blanket-suppress the whole failing set to make CI green.

## How the gate works

`integration-test/scripts/openapi-check.sh`:

1. Copies each service's generated spec into `integration-test/apidefinitions/`.
2. Stages each `<module>/openapi-breaking-allowlist.txt` next to it.
3. Downloads baseline specs from S3 and runs
   `oasdiff changelog <baseline> <current> --color never --err-ignore <allowlist> -o ERR`.

Which baselines it compares against depends on the target branch:

- **on `master`** — the **previous minor** line's latest published build only.
- **on a release line `X.Y.0`** — its **own** line's latest build **and** the previous
  minor (`X.(Y-1).0`) latest build.

`-o ERR` means the gate **only fails on `error`-level** oasdiff changes. `warning` and
`info` entries (e.g. `request-property-removed`, `response-optional-property-removed`,
`response-property-enum-value-removed`, `api-schema-removed`) are reported but do **not**
fail the build. In practice the failing classes are things like
`request-property-enum-value-removed`, endpoints/required-params added, type changes — read
the summary line `N changes: E error, W warning, I info`; only the `error` rows matter.

## Prefer not to break (Option 1)

Add the new field/endpoint, mark the old one `deprecated: true`, and remove it only after
the deprecation window. This never trips the gate and needs no allowlist entry. Reach for
the allowlist only for a real removal / incompatible change that must land now.

## Run the check locally

Requires **Docker** and network access to S3 (baselines) and the release API.

```bash
cd integration-test

# 1. Generate the OpenAPI specs (runs each module's OpenApiGenerator test →
#    <module>/build/openapi/<svc>.json). Slow; skip if specs are already built.
./scripts/build-swagger.sh

# 2. Run the full compatibility gate exactly as CI does.
#    CB_VERSION mimics the build being tested (e.g. the current line + a build number);
#    CB_TARGET_BRANCH selects the baseline set (master vs a release line).
INTEGCB_LOCATION="$PWD/scripts" \
CB_VERSION=2.114.0-b1 \
CB_TARGET_BRANCH=master \
  ./scripts/openapi-check.sh
```

Look for `COMPATIBILITY BREAKS in <service>` blocks and the final `Incompatible changes:`
list. `CHANGE IS COMPATIBLE` / exit 0 means the gate passes.

### Fast single-service iteration

Once specs exist under `apidefinitions/`, diff one service directly instead of re-running
the whole script (image name comes from `DOCKER_SSC_IMAGE` in `service-registry.sh`):

```bash
IMG=docker-private.infra.cloudera.com/cloudera_thirdparty/tufin/oasdiff:v1.10.27
cd integration-test
docker run --rm -t -v "$PWD/apidefinitions:/apidefinitions" "$IMG" \
  changelog /apidefinitions/datalake-openapi-<baseline-build>.json \
            /apidefinitions/datalake.json \
  --color never -o ERR
# add: --err-ignore /apidefinitions/datalake-breaking-allowlist.txt   to test suppression
```

The end-to-end test `integration-test/scripts/test-openapi-allowlist.sh` exercises
detection → suppression → stale-pruning against a real baseline and is the cheapest way to
confirm the mechanism still works.

## Generate an allowlist entry

1. Get the exact breaking-change text. Either run locally (above) or open the failed
   **Swagger Compatibility** job on the PR and find the `COMPATIBILITY BREAKS in <service>`
   block in its log.
2. For **each `error` row**, join the `in API <METHOD> <path>` line with its indented
   description into a **single line**. oasdiff's `--err-ignore` does a substring match on
   that one-line form. Example (from the two-line oasdiff output):
   ```
   in API POST /sdx/{name} removed the enum value 'WASB' of the request property 'cloudStorage/fileSystemType'
   ```
3. Append one line per break to the allowlist of the **owning module**:
   `<module>/openapi-breaking-allowlist.txt`. Commit it on the branch where the break
   should **first ship** (normally a PR against `master`) — never seed it into an older
   line.

Only `error`-level breaks need entries; don't add lines for warnings/info.

### ⚠ Gotchas

- The line must be the **full** message including the `in API <METHOD> <path> …` prefix. A
  partial line matches nothing.
- **Never** paste a full breaking-change message into a `#` comment — a `#` line is inert
  only because it isn't a complete message; a comment that happens to contain one will
  silently suppress that change.
- Blank lines are skipped.

## Service → module mapping

From `service-registry.sh` (owning module = where its allowlist lives):

| service | module |
|---|---|
| cloudbreak | `core` |
| freeipa | `freeipa` |
| environment | `environment` |
| datalake | `datalake` |
| redbeams | `redbeams` |
| autoscale | `autoscale` |
| remoteenvironment | `environment-remote` |
| externalizedcompute | `externalized-compute` |
| maintenance | `maintenance` |

## Cleanup is automatic

An allowlist entry becomes stale once *that branch's* baseline build already contains the
changed shape (oasdiff then finds no break even without the entry). On **master**, an entry
added at minor N is required for the whole N cycle and becomes droppable only after the
minor roll. The `openapi-allowlist-cleanup` GitHub workflow
(`.github/workflows/openapi-allowlist-cleanup.yaml`) runs daily on master, detects stale
entries, and opens a PR removing them — you don't track this manually.
