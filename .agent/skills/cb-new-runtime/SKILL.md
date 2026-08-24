---
name: cb-new-runtime
description: Introduce a new Cloudera Runtime (CR) version in Cloudbreak and later make it the default — Data Lake + Data Hub blueprints, cluster templates, application.yml runtime properties, upgrade matrix, and the tests that must be bumped. Use when Release Engineering asks to add a new CR (e.g. 7.3.6) or to promote it to default.
---

# Introduce a new Cloudera Runtime version in Cloudbreak

Cloudbreak uses a **base + overlay** model for runtime templates. One runtime — **7.3.3**
(`RuntimeOverlayConstants.BASE_VERSION`) — is frozen as full files on disk. Every runtime **newer
than the base ships as a sparse overlay**: the full blueprint / cluster-template / duty set is
reconstructed at startup from the base + any deltas + automatic version-string injection. Nothing is
copied per CR and nothing is generated to disk.

So adding a CR is usually **a few config-line edits, not a directory copy.** In the common case where
the new CR is identical to the base except for the version strings, you author **zero** template
files.

- **Phase 1 — Introduce the runtime** (land as soon as RE raises the Jira). Section 1.
- **Phase 2 — Make it the default** (only after a prewarmed prod image exists and the CR is stable,
  else e2e goes red). Section 2.

Out of scope: image-burning / catalog promotion (not a CB-repo concern); the CDP CLI/API runtime
enum (external `thunderhead` repo); versions **≤ 7.3.3**, which stay frozen full dirs — never touch
or convert them. Promoting a *newer* version to be the frozen base is a rare, separate operation, not
this skill.

> Underlying mechanics: the base+overlay engine (resolver, four flavors, version injection) is
> documented in `service-common/.../common/runtime/overlay/README.md`; the RFC 6902 patch rules and
> the `name=value` selector in `common/.../common/json/patch/README.md`. Each module's adapter has its
> own README next to the loader (`core/.../init/{blueprint,clustertemplate}/overlay/`,
> `datalake/.../configuration/overlay/`).

## Placeholders

`<NEW_CR>` = the version being added (must be **> 7.3.3**). `<BASE>` = `7.3.3`. Confirm the base and
current top before editing (do not hard-code — they move):

```bash
grep -n 'BASE_VERSION' common/src/main/java/com/sequenceiq/cloudbreak/common/runtime/overlay/RuntimeOverlayConstants.java
grep -n 'latest:\|patched:' core/src/main/resources/application.yml       # cb.runtimes.latest / patched
grep -n 'supported:\|advertised:' datalake/src/main/resources/application.yml
```

---

## Phase 1 — Introduce the new runtime

### 1.1 Register `<NEW_CR>` as an overlay version (always required)

The whole overlay model ships behind a kill-switch that is **off by default**: set
`cb.runtimes.overlay.enabled: true` in **both** `core` and `datalake` `application.yml` — otherwise the
loaders never run and `<NEW_CR>` will not materialize regardless of the lists below. (The switch exists so
the model can be disabled in production without a code rollback; leave it off only when reverting.)

Two modules own two different property sets — **both** must list `<NEW_CR>`:

1. **`core/src/main/resources/application.yml`** under `cb.runtimes:`
   - Append `<NEW_CR>` to `patched` (e.g. `patched: "<NEW_CR>"`, comma-separated if the list is non-empty). This drives the
     **blueprint** and **cluster-template** overlays (`DefaultBlueprintCache` /
     `DefaultClusterTemplateCache`).
   - Set `latest: "<NEW_CR>"` to the newest advertised version. **This is mandatory** — see the
     gotcha below; skip it and the runtime materializes but is invisible in the UI.
2. **`datalake/src/main/resources/application.yml`** under `datalake.runtimes:`
   - Append `<NEW_CR>` to **`supported`** (drives the **duty** overlays) **and** to **`advertised`**
     (makes it selectable in the UI).
   - Do **not** touch `datalake.runtimes.default` — that is Phase 2.

The frozen base version is `RuntimeOverlayConstants.BASE_VERSION` (7.3.3). It can be overridden with
`cb.runtimes.base` (all overlay loaders read it), but only re-point it at a version whose **full** on-disk
template dirs already ship (`defaults/blueprints/<base>/`, `defaults/clustertemplates/<base>/`,
`duties/<base>/`) — a base with no on-disk dir fails every cache at startup. Promoting the base is a rare,
deliberate step, separate from adding `<NEW_CR>`.

If `<NEW_CR>` is identical to the base modulo version strings (the common case), **you are done with
templates** — skip 1.2. The three loaders reconstruct the full set from the base + version injection.

### 1.2 Author overlay deltas (only if `<NEW_CR>` genuinely differs from base)

Drop delta files under `runtime-overlays/<NEW_CR>/<subtree>/` in the owning module's
`src/main/resources`:

| subtree           | module      | path within subtree                | example                                   |
|-------------------|-------------|------------------------------------|-------------------------------------------|
| `blueprints`      | **core**    | flat `<stem>`                      | `blueprints/cdp-data-engineering-spark3.*`|
| `clustertemplates`| **core**    | `<provider>/<template>` (aws/azure/gcp/yarn) | `clustertemplates/aws/dataengineering-spark3.*` |
| `duties`          | **datalake**| `<platform>/<shape>`               | `duties/aws/medium_duty_ha.*`             |

There are **four** delta kinds (see the README for the resolver rules):

- **Patch** `<path>.patch.json` — an RFC 6902 array modifying a base file. **Every `replace`/`remove`
  must be immediately preceded by a `test` op on the same path** (enforced — base drift fails loud).
  Address array elements by `name=<value>`, not index:
  ```json
  [
    { "op": "test",    "path": "/instanceGroups/name=core/template/instanceType", "value": "m5.2xlarge" },
    { "op": "replace", "path": "/instanceGroups/name=core/template/instanceType", "value": "m5.8xlarge" }
  ]
  ```
- **Tombstone** `<path>.tombstone` — an empty file; drops that base file for this version.
- **Addition** — a whole new template the base never had: drop the plain `<path>.json` (cluster
  template / duty) or `<path>.bp` (blueprint). In its version-carrying fields write the placeholder
  `__RUNTIME_VERSION__` instead of a concrete version, e.g. `"__RUNTIME_VERSION__ - Brand New: Foo"`;
  the loader swaps it for the actual runtime version. **For a new blueprint also add a `<stem>.name`
  sidecar** containing the display name with the same placeholder, e.g.
  `__RUNTIME_VERSION__ - Brand New: Foo`.
- **Nothing** — zero-patch; handled entirely by 1.1.

**Never hand-author a concrete version** in `name`, `description`, `cdhVersion` or `blueprintName`:
for base files the engine swaps the base-version prefix; for additions write the `__RUNTIME_VERSION__`
placeholder and the engine substitutes the target version. A file must never carry a hard-coded
`7.3.x` in these fields. A patch anchored at `<NEW_CR>` forward-propagates into every higher version
(highest anchor ≤ V wins); a one-off fix for a single later version is a counter-patch anchored there.

### 1.3 Upgrade matrix (independent of the overlay model)

1. Add the `<NEW_CR>` entry to `core/src/main/resources/definitions/upgrade-matrix-definition.json`.
   The allowed *source* versions are a **product decision — ask RE / the upgrade owners**, don't guess.
2. Bump the expected size in `RuntimeUpgradeMatrixDefinitionProviderTest` (currently
   `assertEquals(10, ...)`) — `core/src/test/java/com/sequenceiq/cloudbreak/service/upgrade/matrix/RuntimeUpgradeMatrixDefinitionProviderTest.java`.

### 1.4 Fix the count oracle

`ClusterTemplateTest.validateDefaultCount` asserts the total default cluster-template count
(currently `905`) — `integration-test/src/main/java/com/sequenceiq/it/cloudbreak/testcase/mock/ClusterTemplateTest.java`.
A new zero-patch version contributes the base's full count (**+110** at the base's current shape),
minus any tombstones you added and templates whose blueprint is gov-only. Run the test and let it
report the exact number rather than eyeballing it.

---

## Phase 2 — Make the new CR the default (later, separate PR)

**Precondition:** a prewarmed image with `<NEW_CR>` exists in the **prod** image catalog and the CR
is stable. Earlier makes e2e extremely flaky.

1. Set `datalake.runtimes.default: "<NEW_CR>"` in `datalake/src/main/resources/application.yml`.
2. In `integration-test/src/main/resources/application.yml` set the **target/default** ones to `<NEW_CR>`:
   - `integrationtest.runtimeVersion`
   - `integrationtest.upgrade.targetRuntimeVersion`
   - `integrationtest.upgrade.distroXUpgradeTargetVersion`
   (`integrationtest.upgrade.currentRuntimeVersion` / source-side values stay on the older CR.)

The image-burn-trigger removal that pairs with promotion is not a CB-repo change — ignore it here.

---

## Verify

```bash
# Overlay engine + adapters (unit)
./gradlew :common:test --tests '*RuntimeOverlay*' --tests '*JsonPatch*'
./gradlew :core:test   --tests '*RuntimeBlueprintOverlayLoaderTest' --tests '*RuntimeClusterTemplateOverlayLoaderTest' \
                       --tests '*RuntimeUpgradeMatrixDefinitionProviderTest'
./gradlew :datalake:test --tests '*RuntimeDutyOverlayLoaderTest' --tests '*CDPConfigServiceTest'
# ClusterTemplateTest is an integration-test mock case — run per your usual integration-test invocation
# to read the real default count.
```

Then load **cb-testing** (coverage gates + authorization-compliance) and **cb-jira** to link the ticket.

## Gotchas

- **UI-invisibility (the big one):** a patched runtime materializes, caches, and persists correctly
  but the DataHub blueprint **list API returns 0** for it unless `cb.runtimes.latest` is bumped to the
  newest advertised version. `SupportedRuntimes.isSupported` filters out anything newer than `latest`.
  Bumping `cb.runtimes.latest` in `core` `application.yml` is **not optional**.
- **Kill-switch off by default:** `cb.runtimes.overlay.enabled` defaults to `false`, so the loaders are
  dormant until you flip it to `true` (in **both** `core` and `datalake`). List a version but leave the
  switch off and nothing materializes — no error, just an empty overlay.
- **Two modules, two property sets:** core `cb.runtimes.patched` (blueprints + cluster templates) vs
  datalake `runtimes.supported` + `advertised` (duties + UI). List the new version in **both** or you
  get a half-populated runtime.
- **Base drift fails loud:** editing a base 7.3.3 file that an overlay patches makes that patch's
  guarding `test` op throw at startup. That's the safety net working — re-anchor the patch.
- **Don't author version strings** in overlay files — injection owns `name` / `description` /
  `cdhVersion` / `blueprintName`. A typo'd `blueprintName` in a hand-copied file silently yields zero
  templates.
- **Count assertions fail by design** until bumped (upgrade-matrix size, `validateDefaultCount`). Let
  the failing test tell you the number.
- **Versions ≤ 7.3.3 are frozen full dirs** — leave them; the overlay model is forward-only.
