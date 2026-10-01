---
name: cb-runtime-template-change
description: Change a Cloudbreak runtime template (Data Hub blueprint, Data Hub cluster template, or Data Lake duty) for an overlay version by authoring a sparse delta against the frozen base, instead of editing a full copy. Covers the five delta flavors and the typical scenarios — single patch, patches across commits, patch-then-replace, whole-file addition, tombstone, structural replace — plus how to render and diff the result. Use when a ticket asks to tweak/add/remove a template for a runtime newer than the base.
---

# Change a runtime template (base + overlay)

Cloudbreak reconstructs the blueprint / cluster-template / duty set for every runtime **newer than the
base** in memory, from the frozen base plus a sparse **overlay** of deltas, plus automatic version-string
injection. One runtime — **`7.3.3`** (`RuntimeOverlayConstants.BASE_VERSION`) — is the frozen base: full
files on disk. Versions `≤ 7.3.3` are frozen full dirs; **never** touch or convert them.

So "change template X for runtime Y" means **author a delta under `runtime-overlays/<Y>/…`**, not edit a
full file. This skill is the per-scenario runbook for that. It is distinct from **cb-new-runtime** (which
*introduces* a whole version — config lists, upgrade matrix, count tests). If the version `<Y>` is not
registered yet, do cb-new-runtime's Phase 1 registration first, then come back here for the content change.

> Full narrative + a lot of worked examples: **`README-runtime-template-engine.md`** (repo root). Engine
> mechanics: `service-common/src/main/java/com/sequenceiq/cloudbreak/common/runtime/overlay/README.md`.
> RFC 6902 patch rules + the `name=value` selector: `common/src/main/java/com/sequenceiq/cloudbreak/common/json/patch/README.md`.
> Per-tree adapters: `core/.../init/{blueprint,clustertemplate}/overlay/README.md`,
> `datalake/.../configuration/overlay/README.md`.

## Before you touch anything — orient

Confirm the base and which versions are overlays (do **not** hard-code — they move):

```bash
grep -n 'BASE_VERSION' service-common/src/main/java/com/sequenceiq/cloudbreak/common/runtime/overlay/RuntimeOverlayConstants.java
grep -n 'patched:' core/src/main/resources/application.yml            # Data Hub overlay versions
grep -n 'supported:' datalake/src/main/resources/application.yml      # Data Lake overlay versions
```

The three template trees and where their deltas go:

| Tree | Base files | Overlay deltas |
|------|-----------|----------------|
| Data Hub blueprints (`.bp`) | `core/.../defaults/blueprints/<base>/` | `core/.../runtime-overlays/<v>/blueprints/` |
| Data Hub cluster templates (`.json`) | `core/.../defaults/clustertemplates/<base>/` | `core/.../runtime-overlays/<v>/clustertemplates/<provider>/` |
| Data Lake duties (`.json`) | `datalake/.../duties/<base>/` | `datalake/.../runtime-overlays/<v>/duties/<provider>/` |

Find the base file you are changing, and look at how any existing overlay already deltas it, before you write:

```bash
ls core/src/main/resources/defaults/blueprints/<base>/
find */src/main/resources/runtime-overlays -type f    # existing deltas to mirror in style
```

## The five delta flavors

Pick the **smallest** flavor that expresses the change. One file per changed base file.

| Flavor | File name | Use when |
|--------|-----------|----------|
| **zero-delta** | *(no file)* | the version is identical to the base but for version strings |
| **patch** | `<base-name>.patch.json` | change a few fields of an existing base template (RFC 6902) |
| **tombstone** | `<base-name>.tombstone` | the base template must **not** exist in this version |
| **addition** | `<new-name><suffix>` (+ `<stem>.name` for blueprints) | a template the base never had |
| **replace** | `<base-name>.replace<suffix>` | the change is so structural a patch would be unreadable |

`<suffix>` is `.bp` for blueprints, `.json` for cluster templates and duties. **Prefer `patch` over
`replace`** — a patch documents intent and survives base edits; a replace freezes a full copy and silently
drifts from the base. Reach for `replace` only as an escape hatch.

## Version-string injection — do not hand-write versions

The engine injects the target version for you, so author deltas **version-neutrally**:

- **Patches on base files**: the base version prefix in injected pointers is swapped automatically. Do not
  write the target version into patched values that are version-carrying fields.
- **Additions / replacements**: write the literal `__RUNTIME_VERSION__` placeholder wherever the version
  belongs (e.g. a blueprint `.name` sidecar: `"__RUNTIME_VERSION__ - Brand New: Foo"`); it is substituted at load.
- Injected pointers per tree: blueprints `/description`, `/blueprint/cdhVersion`; cluster templates
  `/name`, `/distroXTemplate/cluster/blueprintName`; duties `/cluster/blueprintName`.

## Authoring RFC 6902 patches

- Start every patch with a **`test`** op that pins the base value you are changing, so the patch fails loud
  if the base moves out from under it instead of silently misapplying.
- Address array elements by a **`<field>=<value>` selector**, never a positional index
  (`/.../roles/name=NIFI/...`), and use the **minimal** op (a single `add`, not a whole-array `replace`).
- Keep the patch to the fields that actually change.

```json
[
  { "op": "test",    "path": "/instanceGroups/name=worker/template/instanceType", "value": "r5.2xlarge" },
  { "op": "replace", "path": "/instanceGroups/name=worker/template/instanceType", "value": "r5.4xlarge" }
]
```

## Scenarios (match the README examples)

1. **Zero-delta** — version identical to the base. Author **no file**; just register the version
   (cb-new-runtime). The engine serves the base with version strings swapped.
2. **Single patch** — one field of one template. Add `<name>.patch.json` with a `test` + the minimal op.
3. **Addition** — a template the base lacks. Drop the whole file `<new-name><suffix>` under the overlay;
   for a **blueprint** also add a `<stem>.name` sidecar with the display name using `__RUNTIME_VERSION__`.
4. **Multiple patches across commits** — overlays are **cumulative and additive**; a later version's
   `runtime-overlays/<later>/…` only needs *its own* new delta. Each commit adds files; nothing is rewritten.
5. **Patch then replace / reset** — if a later version must undo an earlier patch's field, patch it back to
   the base value (don't delete history). To stop deltaing a file entirely from a version on, remove that
   version's delta file so it falls back to the base.
6. **Tombstone (and revive)** — a base template that must not exist in this version: add `<name>.tombstone`.
   To bring it back in a still-later version, that version simply carries no tombstone (and a patch/addition
   if it also changed).
7. **Structural replace** — last resort for a wholesale rewrite: `<base-name>.replace<suffix>` holding the
   full new file with `__RUNTIME_VERSION__` where the version goes. Note in the commit why a patch wouldn't do.

## Checklist before you open the PR

- [ ] Smallest flavor used; `patch` preferred over `replace`.
- [ ] Patches lead with a `test` op; array elements addressed by `name=value`, minimal ops only.
- [ ] No hand-written target version — `__RUNTIME_VERSION__` in additions/replacements, injection trusted elsewhere.
- [ ] Blueprint additions ship a `<stem>.name` sidecar.
- [ ] Version registered (cb-new-runtime) if this is its first delta; count-assertion tests bumped if the live template set changed.
- [ ] Nothing generated under `build/` is committed.
