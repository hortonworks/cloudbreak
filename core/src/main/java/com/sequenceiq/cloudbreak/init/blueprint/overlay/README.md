# Blueprint runtime overlay (core adapter)

`RuntimeBlueprintOverlayLoader` materializes the default **blueprints** (`.bp` files) for runtime
versions that ship as overlays, so `defaults/blueprints/<version>/` need not be copied per CR. It is a
thin adapter over the shared engine `RuntimeOverlayResolver` (in `service-common`, package
`com.sequenceiq.cloudbreak.common.runtime.overlay`) — see that engine's README for the resolution
algorithm and the four overlay flavors. This file documents only what is specific to blueprints.

## What this adapter supplies

| engine parameter    | value here                                             |
|---------------------|--------------------------------------------------------|
| `baseSubtree`       | `defaults/blueprints`                                  |
| `overlaySubtree`    | `blueprints`                                           |
| `baseFileSuffix`    | `.bp` (not the default `.json`)                        |
| file filter         | `^[a-z0-9-]+\.bp$` (a flat `<stem>.bp`)                |
| injection pointers  | `/description` and `/blueprint/cdhVersion`             |

The result is merged into `DefaultBlueprintCache` after the disk scan with **on-disk wins**.

## The blueprint-only wrinkle: the display name is external

A blueprint's DB identity — its display name — is **not a field inside the `.bp` file**. It is the
left-hand side of a `displayName=fileStem` entry in the `cb.blueprint.cm.defaults.<version>` YAML
block. So this loader recovers the `fileStem → displayName` mapping and derives each overlay version's
name by swapping only the `"<baseVersion> "` prefix (never a blind rewrite). Two sources feed it:

- **existing blueprints** — the base version's YAML block; and
- **added blueprints** (a stem the base never had) — a companion **`<stem>.name` sidecar** next to the
  `.bp` addition, holding the base-prefixed display name (e.g. `7.3.3 - Brand New: Foo`).

The base block wins on conflict. A `.bp` with neither a base registration nor a `.name` sidecar is
skipped (there is no name to register it under). Each materialized blueprint carries its synthesized
name, its file stem (the key the gov-cloud exclusion filter uses), and the version-injected `.bp` JSON.

## Where deltas live

`core/src/main/resources/runtime-overlays/<version>/blueprints/<stem>.*` — e.g.
`runtime-overlays/7.3.6/blueprints/cdp-data-engineering-spark3.patch.json`, and for an addition,
`cdp-brand-new.bp` **plus** `cdp-brand-new.name`.

## Which versions are overlays

Driven by `cb.runtimes.patched` in `core/src/main/resources/application.yml` (shared with the
cluster-template overlay loader).

> Do not hand-author `/description` or `/blueprint/cdhVersion` in overlay files — the engine injects
> the version. See the module workflow in the `cb-new-runtime` skill.

Tests: `RuntimeBlueprintOverlayLoaderTest` (real base + `runtime-overlays/7.3.6` fixture incl. the
sidecar and an orphan `.bp`) and `DefaultBlueprintCacheTest` (the merge branches).
