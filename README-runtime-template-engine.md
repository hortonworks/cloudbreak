# Runtime template engine — base + overlay (dev guide)

A practical guide to changing Cloudbreak's default **runtime templates** (Data Hub blueprints, cluster
templates, and Data Lake duties) under the base + overlay model. For the design and the exact
resolution algorithm, see the reference READMEs linked at the end; this document is the task-first
entry point: *"I need to change a template for a runtime — what do I actually do?"*

---

## What the engine does (the short version)

Cloudbreak used to ship a **full copy of every template for every runtime version**: adding a new
runtime meant copying the whole previous version's directory and editing a handful of files, so the
tree grew by hundreds of near-identical files per CR.

The engine replaces that with **one frozen base version plus sparse overlays**:

- **One base** — `7.3.3` (`RuntimeOverlayConstants.BASE_VERSION`) — keeps its full template files on disk.
- **Every runtime newer than the base ships only its *delta*** against the base, under
  `runtime-overlays/<version>/…`. At startup the engine reconstructs each version's full template set
  in memory: base files + deltas + the version string injected into the right fields.
- **Nothing is copied per CR, and nothing is generated to disk.** In the common case where a new
  version is identical to the base except for the version strings, you author **zero** files — you
  just list the version.

Versions **≤ 7.3.3** stay as frozen full directories; the engine never touches them. The whole model
sits behind a kill-switch, `cb.runtimes.overlay.enabled` (default `false`), so it can be turned off in
production without a code rollback.

### The three template trees

| tree | base on disk | overlay subtree | key fields the version is injected into |
|------|--------------|-----------------|------------------------------------------|
| **Data Hub blueprints** (`.bp`) | `core/.../defaults/blueprints/<base>/` | `runtime-overlays/<v>/blueprints/` | `/description`, `/blueprint/cdhVersion` |
| **Data Hub cluster templates** (`.json`) | `core/.../defaults/clustertemplates/<base>/` | `runtime-overlays/<v>/clustertemplates/<provider>/` | `/name`, `/distroXTemplate/cluster/blueprintName` |
| **Data Lake duties** (`.json`) | `datalake/.../duties/<base>/` | `runtime-overlays/<v>/duties/<platform>/` | `/cluster/blueprintName` |

### The five overlay flavors

A delta is one of five things, chosen by **file name**:

| you want to… | author | notes |
|--------------|--------|-------|
| nothing (version == base but for version strings) | *(no file)* — just list the version | **zero-delta** |
| tweak a few fields of a base file | `<path>.patch.json` | RFC 6902 patch; the default verb |
| add a brand-new file the base never had | `<path><suffix>` (e.g. `cdp-foo.bp`) | **addition**; version fields use `__RUNTIME_VERSION__` |
| rewrite a base file wholesale (structural) | `<path>.replace<suffix>` | **replacement**; escape hatch, version fields use `__RUNTIME_VERSION__` |
| drop a base file for this version | `<path>.tombstone` | empty marker file |

> **The default verb is `.patch.json`.** It is a reviewable delta, its mandatory `test` guards detect
> base drift, and the file keeps inheriting later base changes. Reach for `.replace` only when the
> patch stops being a readable delta (see example 7).

---

## Before you start

Confirm the base and which versions are overlays (these move — don't hard-code them):

```bash
grep -n 'BASE_VERSION'  service-common/src/main/java/com/sequenceiq/cloudbreak/common/runtime/overlay/RuntimeOverlayConstants.java
grep -n 'patched:'      core/src/main/resources/application.yml        # cb.runtimes.patched  (blueprints + cluster templates)
grep -n 'supported:'    datalake/src/main/resources/application.yml     # datalake.runtimes.supported  (duties)
```

A version is materialized as an **overlay** only when it is listed, is **newer than the base**, and has
**no** full on-disk directory of its own. Core and Data Lake own **two different lists** — a new
version that needs both Data Hub and Data Lake templates must be listed in **both**.

---

## Worked examples

All examples add deltas for a **hypothetical** overlay version **`7.3.4`** on top of base **`7.3.3`**;
the later examples reference further hypothetical versions (`7.3.5`, `7.3.6`, `7.3.7`) to show how
deltas compose across versions.

> **None of these versions exist today.** At the time of writing the newest real runtime *is* the base,
> `7.3.3`, and the production `runtime-overlays/` tree is empty — so `7.3.4`+ are placeholders for
> whatever future runtime you are actually working on. Substitute your real target version throughout.

### 1. Zero-delta — a new version identical to the base

The new runtime is the base with different version strings. Author **no files at all**; just register
the version.

```yaml
# core/src/main/resources/application.yml
cb:
  runtimes:
    patched: "7.3.4"          # comma-separated if the list is non-empty
```
```yaml
# datalake/src/main/resources/application.yml
datalake:
  runtimes:
    supported: "...,7.3.3,7.3.4"
```

The engine serves every base template with `7.3.3` swapped to `7.3.4` in the injected fields (e.g. a
blueprint `/description` of `"7.3.3 - Data Engineering"` becomes `"7.3.4 - Data Engineering"`). The version-string swap is covered by the engine's own tests.

### 2. Single patch — change one field of one template

7.3.4 should bump the core instance type of the AWS Data Engineering duty. Author one patch:

```jsonc
// datalake/src/main/resources/runtime-overlays/7.3.4/duties/aws/medium_duty_ha.patch.json
[
  { "op": "test",    "path": "/instanceGroups/name=core/template/instanceType", "value": "m5.2xlarge" },
  { "op": "replace", "path": "/instanceGroups/name=core/template/instanceType", "value": "m5.8xlarge" }
]
```

Rules that matter:

- **Every `replace`/`remove` is immediately preceded by a `test` on the *same path*.** The `test` is
  drift detection: if the base value changes underneath your patch, the build fails loud instead of
  silently producing the wrong template.
- **Address array elements by field, not index** — `/instanceGroups/name=core/…` selects the element
  whose `name` is `core`, so it stays correct when the array is reordered.
- **Keep the op minimal** — change only what moves. Don't `replace` a whole array to touch one entry.

### 3. Addition — a brand-new template the base never had

7.3.4 introduces a Data Hub blueprint `cdp-brand-new` that does not exist in `7.3.3`. An addition is a
whole file, named exactly like a base file (no `.patch`/`.replace`), with version fields written as the
`__RUNTIME_VERSION__` placeholder (never a concrete version):

```jsonc
// core/src/main/resources/runtime-overlays/7.3.4/blueprints/cdp-brand-new.bp
{
  "description": "__RUNTIME_VERSION__ - Brand New: Streaming",
  "blueprint": { "cdhVersion": "__RUNTIME_VERSION__", "...": "..." }
}
```

Blueprints carry their display name **outside** the `.bp` file, so an added blueprint needs a companion
`.name` sidecar (cluster templates and duties need no sidecar):

```
// core/src/main/resources/runtime-overlays/7.3.4/blueprints/cdp-brand-new.name
__RUNTIME_VERSION__ - Brand New: Streaming
```

Guard rails the engine enforces: an **addition must not** collide with a base path (that would be a
patch or a replacement), and it **forward-propagates** — once added at 7.3.4 it exists in 7.3.5, 7.3.6,
… too, until a tombstone drops it.

### 4. Multiple patches — same file, later version / later commit

Patches **compose and forward-propagate**. Say example 2 landed at 7.3.4. Later, 7.3.5 needs the same
duty to also grow its worker count. You do **not** re-edit the 7.3.4 patch — you add a *new* patch at
7.3.5:

```jsonc
// datalake/src/main/resources/runtime-overlays/7.3.5/duties/aws/medium_duty_ha.patch.json
[
  { "op": "test",    "path": "/instanceGroups/name=worker/nodeCount", "value": 3 },
  { "op": "replace", "path": "/instanceGroups/name=worker/nodeCount", "value": 5 }
]
```

How the engine resolves `medium_duty_ha` for each version:

- **7.3.4** = base + the 7.3.4 patch (instance type → `m5.8xlarge`).
- **7.3.5** = base + the 7.3.4 patch **+** the 7.3.5 patch (instance type → `m5.8xlarge` *and*
  worker count → 5). Patches anchored at `≤ V` apply in ascending version order; a change introduced
  at a lower version flows forward into every higher one.

The same holds for **multiple patches landed in different commits for the same version** — two separate
`*.patch.json` files are not possible for one base file (one file per path), so within a single version
you extend the *existing* `medium_duty_ha.patch.json` by appending ops in a new commit. Across versions,
you add a new file under the new version's directory. If two patches on the same path exist at different
anchors, the **highest anchor wins** for that path.

### 5. Patch then replace — the reset

7.3.4 patches a cluster template; then 7.3.6 rewrites that same template wholesale (see example 7 for
*when*). The replacement **resets** the file:

```
runtime-overlays/7.3.4/clustertemplates/aws/datamart.patch.json      # small delta on the 7.3.3 body
runtime-overlays/7.3.6/clustertemplates/aws/datamart.replace.json    # whole new body
```

Resolution:

- **7.3.4 / 7.3.5** = base `datamart` + the 7.3.4 patch.
- **7.3.6+** = the **replacement body** (version-injected). The 7.3.4 patch is **dropped**, not replayed —
  it was written against the old base body that no longer exists. Only patches anchored at **the same or
  a higher** version than the replacement would still apply, on top of the new body.

So a replacement is a clean break: everything below it on that file is forgotten; everything at or above
it still composes.

### 6. Tombstone — drop a base file (and reviving it)

7.3.4 should no longer ship the `cdp-dlm` blueprint that the base has. Drop it with an empty tombstone
named after the base file:

```bash
touch core/src/main/resources/runtime-overlays/7.3.4/blueprints/cdp-dlm.tombstone
```

From 7.3.4 forward, `cdp-dlm` is absent. To **revive** it later with a new body — say 7.3.7 brings it
back, rebuilt — ship an **addition or replacement anchored above the tombstone**:

```
runtime-overlays/7.3.4/blueprints/cdp-dlm.tombstone        # gone from 7.3.4
runtime-overlays/7.3.7/blueprints/cdp-dlm.bp               # back from 7.3.7, new body (+ .name sidecar)
```

A whole-file verb anchored above a tombstone resets the file and brings it back; the tombstone only
governs the range below that anchor.

### 7. Replace — the structural-rewrite escape hatch

Use `.replace` **only** when a patch stops being a reviewable delta. Two questions decide it:

- **Is the patch still a delta a reviewer can read against intent?** A duty that drops one instance
  group and resizes two others is 3–5 ops that read exactly like the change — **keep the patch**, however
  long. A blueprint that moves whole role-config-group sets between host templates becomes ~180 lines of
  guarded `test`+`replace` pairs over reordered arrays that nobody can check against intent — **replace it.**
- **How many files would it multiply into?** A per-provider tree (duties, cluster templates) costs one
  wholesale copy *per provider*; a single blueprint costs one.

```jsonc
// core/src/main/resources/runtime-overlays/7.3.4/clustertemplates/aws/datamart.replace.json
{
  "name": "__RUNTIME_VERSION__ - Data Mart",
  "distroXTemplate": { "cluster": { "blueprintName": "__RUNTIME_VERSION__ - Data Mart", "...": "..." } }
}
```

What a replacement costs — and why it is not the default:

- **no drift detection** — it carries no `test` ops, so a later change to the base goes unnoticed;
- **no future base inheritance** — the file stops tracking the base from its anchor onward; a later base
  fix must be re-applied by hand (or by a patch anchored above the replacement);
- **no visible delta** — a reviewer sees a whole file, not what changed.

The win is reviewability, never size — the whole file is usually *larger* than the patch it replaces.

---

## Version injection — write the placeholder, not a version

Injection is field-targeted, never a blind string replace, and it treats the two authoring forms
differently:

- **Base files** carry the literal base version (`"7.3.3 - Data Engineering"`); the engine swaps the
  `7.3.3` prefix / bare leaf forward to the target version.
- **Additions and replacements** (bodies for versions newer than the base) must write
  `__RUNTIME_VERSION__` in the injected fields — the engine swaps every occurrence for the target.
  **Never hand-author a concrete version** in an addition/replacement; a hard-coded `7.3.4` becomes a lie
  the moment the file forward-propagates to 7.3.5.

Only the caller-named pointers (the table above) are rewritten. A value that merely *contains* the base
number — a parcel URL, an embedded component version — is left untouched.

---

## Checklist for a template change

1. Pick the flavor (table above). Default to `.patch.json`.
2. Author the delta under `runtime-overlays/<version>/<tree>/…`; use `__RUNTIME_VERSION__` in
   additions/replacements; guard every `replace`/`remove` with a `test`.
3. List the version — `cb.runtimes.patched` (core) and/or `datalake.runtimes.supported` (datalake).
4. Run the overlay + cache tests (see the `cb-runtime-template-change` skill for the exact set).

For a step-by-step runbook, use the **`cb-runtime-template-change`** Claude skill
(`.agent/skills/cb-runtime-template-change/`); to introduce or promote a whole CR version, use
**`cb-new-runtime`**.

---

## Reference

- Wiki mirror (same content, for discoverability — this in-repo guide is the source of truth): [Runtime Template Engine (base + overlay) — Developer Guide](https://cloudera.atlassian.net/wiki/spaces/ENG/pages/12388140048/Runtime+Template+Engine+base+overlay+Developer+Guide)
- Engine (resolver, algorithm, five flavors, injection): `service-common/src/main/java/com/sequenceiq/cloudbreak/common/runtime/overlay/README.md`
- JSON patch (ops, the `field=value` selector, the guard rule): `common/src/main/java/com/sequenceiq/cloudbreak/common/json/patch/README.md`
- Blueprint adapter: `core/src/main/java/com/sequenceiq/cloudbreak/init/blueprint/overlay/README.md`
- Cluster-template adapter: `core/src/main/java/com/sequenceiq/cloudbreak/init/clustertemplate/overlay/README.md`
- Duty adapter: `datalake/src/main/java/com/sequenceiq/datalake/configuration/overlay/README.md`
