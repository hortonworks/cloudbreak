---
name: cb-new-runtime
description: Introduce a new Cloudera Runtime (CR) version in Cloudbreak and later make it the default — Data Lake + Data Hub blueprints, cluster templates, application.yml runtime properties, upgrade matrix, and the tests that must be bumped. Use when Release Engineering asks to add a new CR (e.g. 7.3.3) or to promote it to default.
---

# Introduce a new Cloudera Runtime version in Cloudbreak

Adding a CR is a **manual, copy-heavy checklist** — there is no generator. The safe pattern for every step is *copy the former CR's files/entries, then rewrite the version string inside them*. Work is split into two phases that happen at different times:

- **Phase 1 — Introduce the new runtime** (this can land as soon as RE raises the Jira). Sections 1.1 + 1.2 below.
- **Phase 2 — Make it the default** (only after a prewarmed image with the new CR exists in the **prod** image catalog and the CR is stable — otherwise e2e tests go red). Section 2 below.

Out of scope: image-burning and catalog-promotion (test/stage/prod) are **not** a Cloudbreak-repo concern — ignore them here. The CDP CLI/API runtime enum lives in the external `thunderhead` repo, not this one, so it's out of scope for this skill too.

## Version placeholders & current state

Throughout, `<NEW_CR>` = the version being added, `<PREV_CR>` = the highest existing version to copy from. Find `<PREV_CR>` (do not hard-code — it moves every release):

```bash
ls core/src/main/resources/defaults/blueprints/ | grep -E '^7\.' | sort -V | tail -3
grep -n 'latest:' core/src/main/resources/application.yml            # cb.runtimes.latest — the current top CR
```

As of this writing the latest CR is **7.3.2**; use whatever the commands above report. Version dirs are **not** globally consistent (e.g. `7.2.13` was skipped) — always copy from the actual highest dir, never assume `N-1`.

---

## Phase 1.1 — Data Lake runtime

1. **Blueprints.** `cp -r core/src/main/resources/defaults/blueprints/<PREV_CR>/cdp-sdx*.bp` into a new `core/src/main/resources/defaults/blueprints/<NEW_CR>/` dir. In each copied `.bp`, update the `"description"` (line ~5, e.g. `"7.3.2 - SDX template ..."`) and `"cdhVersion"` (e.g. `"cdhVersion": "7.3.2"`) to `<NEW_CR>`.
2. **Register blueprints in core.** Add a `<NEW_CR>:` entry under `cb.blueprint.cm.defaults:` in `core/src/main/resources/application.yml` (around the `cm: defaults:` block). Copy the `<PREV_CR>` block and rewrite every version-prefixed template label + the `<NEW_CR>` key. Each line maps a human-readable name → blueprint file id, e.g. `7.3.2 - SDX Light Duty: ...=cdp-sdx;`.
3. **Data Lake supported / advertised.** In `datalake/src/main/resources/application.yml`, append `<NEW_CR>` to **both**:
   - `datalake.runtimes.supported`
   - `datalake.runtimes.advertised` (this is what makes it selectable in the UI)
   Do **not** touch `datalake.runtimes.default` yet — that is Phase 2.
4. **Duties templates.** `cp -r datalake/src/main/resources/duties/<PREV_CR> datalake/src/main/resources/duties/<NEW_CR>`. In **every** copied JSON, update the `"blueprintName"` fields so their version prefix is `<NEW_CR>` (they must match the blueprint names registered in step 2). Copy all subdirs (`aws`, `azure`, `gcp`, `yarn`, `openstack`, …).
5. **Upgrade matrix.** Add the new version to `core/src/main/resources/definitions/upgrade-matrix-definition.json`. The set of supported *base* versions for a new runtime is a product decision — **ask RE / the upgrade owners**, don't guess the allowed source versions.
6. **Fix the matrix test.** Adding a matrix entry changes its size, so bump the expected count in `RuntimeUpgradeMatrixDefinitionProviderTest.testGetUpgradeMatrixShouldReadTheUpgradeMatrixFromJson` — `core/src/test/java/com/sequenceiq/cloudbreak/service/upgrade/matrix/RuntimeUpgradeMatrixDefinitionProviderTest.java` (currently `assertEquals(9, actual.getRuntimeUpgradeMatrix().size())`).

---

## Phase 1.2 — Data Hub runtime

Data Hubs must run the **exact same** CR version as the Data Lake, so introduce the same `<NEW_CR>` here too.

1. **Blueprints.** Copy the Data Hub `cdp-*.bp` files (everything except the `cdp-sdx*` ones) from `core/src/main/resources/defaults/blueprints/<PREV_CR>/` into `core/src/main/resources/defaults/blueprints/<NEW_CR>/`. Update `"description"` and `"cdhVersion"` in each.
2. **Register blueprints in core.** Same `cb.blueprint.cm.defaults.<NEW_CR>` block in `core/src/main/resources/application.yml` as 1.1 step 2 — the Data Hub blueprint lines live in the same block (one block per CR covers both DL and DH names).
3. **Bump `cb.runtimes.latest`.** Set `cb.runtimes.latest: "<NEW_CR>"` in `core/src/main/resources/application.yml` (currently `7.3.2`).
4. **Cluster templates (per cloud provider).** `cp -r core/src/main/resources/defaults/clustertemplates/<PREV_CR> core/src/main/resources/defaults/clustertemplates/<NEW_CR>` — this covers `aws`, `azure`, `gcp`, `yarn`. (Note the path is `defaults/clustertemplates/`, not a bare `clustertemplates/`.)
5. **Update every copied cluster template.** In each JSON under the new dir, rewrite the version prefix in `"name"` (e.g. `"7.3.2 - Data Engineering Spark3 for AWS"`) and `"blueprintName"` (must match a name registered in step 2).
6. **Service definitions (CDH).** Older docs reference a versioned `template-manager-cmtemplate/.../cloudera-manager-template/cdh/<version>/` layout, but that dir no longer exists in this repo (only `service-definitions-minimal.json` remains). Before assuming there's nothing to do, search for a per-version service-definition file and copy/bump the CDH `version` if one exists for `<PREV_CR>`:
   ```bash
   find template-manager-cmtemplate/src/main/resources -type f | xargs grep -l "<PREV_CR>" 2>/dev/null
   ```
   If nothing version-specific turns up, this step is a no-op for the current layout — note that in the PR rather than fabricating files.
7. **Fix the mock integration test.** Adding cluster templates raises the default-template count, so bump `expectedCount` in `ClusterTemplateTest.validateDefaultCount` — `integration-test/src/main/java/com/sequenceiq/it/cloudbreak/testcase/mock/ClusterTemplateTest.java` (currently `long expectedCount = 783;`). Run the test to get the exact new number rather than eyeballing it.

---

## Phase 2 — Make the new CR the default (later PR)

**Precondition:** a prewarmed image with `<NEW_CR>` is available in the **prod** image catalog and the runtime is stable. Doing this earlier makes e2e tests extremely flaky. This is a separate PR from Phase 1.

1. **Data Lake default.** Set `datalake.runtimes.default: "<NEW_CR>"` in `datalake/src/main/resources/application.yml`.
2. **Integration-test defaults.** In `integration-test/src/main/resources/application.yml` set all three to `<NEW_CR>`:
   - `integrationtest.runtimeVersion`
   - `integrationtest.upgrade.targetRuntimeVersion`
   - `integrationtest.upgrade.distroXUpgradeTargetVersion`
   (`integrationtest.upgrade.currentRuntimeVersion` / source-side values stay on the older CR — only bump the *target*/*default* ones.)

The image-burn-trigger removal that pairs with this promotion is not a CB-repo change — ignore it here.

---

## Verify

```bash
# YAML sanity + the two tests that assert on counts you changed
./gradlew :core:test --tests '*RuntimeUpgradeMatrixDefinitionProviderTest*'
# ClusterTemplateTest is an integration-test mock case — run per your usual integration-test invocation to read the real default count
```

Also load **cb-testing** before opening the PR (coverage gates, authorization-compliance test) and **cb-jira** to link the CR ticket.

## Gotchas

- **`advertised` vs `default` vs `supported`** are three distinct datalake properties. Phase 1 touches `supported`+`advertised`; `default` is Phase 2 only.
- **Copy from the real highest dir**, not `N-1` — versions get skipped.
- **`blueprintName` must match exactly** between duties/cluster-template JSON and the labels registered in `cb.blueprint.cm.defaults.<NEW_CR>`; a typo silently yields zero default templates and fails `validateDefaultCount`.
- **Two count-assertions will fail by design** until you bump them: the upgrade-matrix size and `ClusterTemplateTest` default count. Let the failing test tell you the correct number.
- The whole flow is copy-paste heavy — after copying a dir, `grep -rn '<PREV_CR>'` inside the new dir to catch every version string you still need to rewrite.
