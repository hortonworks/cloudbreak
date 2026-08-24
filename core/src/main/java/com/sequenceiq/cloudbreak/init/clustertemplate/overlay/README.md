# Cluster-template runtime overlay (core adapter)

`RuntimeClusterTemplateOverlayLoader` materializes the default **cluster templates** for runtime
versions that ship as overlays, so `defaults/clustertemplates/<version>/` need not be copied per CR.
It is a thin adapter over the shared engine `RuntimeOverlayResolver` (in `service-common`, package
`com.sequenceiq.cloudbreak.common.runtime.overlay`) — see that engine's README for the resolution
algorithm and the four overlay flavors. This file documents only what is specific to cluster
templates.

## What this adapter supplies

| engine parameter    | value here                                                          |
|---------------------|---------------------------------------------------------------------|
| `baseSubtree`       | `defaults/clustertemplates`                                         |
| `overlaySubtree`    | `clustertemplates`                                                  |
| file filter         | `^(aws\|azure\|gcp\|yarn)/[a-z0-9_-]+\.json$` (one `<provider>/<template>.json`) |
| injection pointers  | `/name` and `/distroXTemplate/cluster/blueprintName`                |

Each materialized template is keyed by its `/name` and serialized back to a raw JSON string, matching
what `DefaultClusterTemplateCache` reads from disk — the cache merges the two with **on-disk wins**
(`putIfAbsent`), so an overlay never shadows a real file.

## Where deltas live

`core/src/main/resources/runtime-overlays/<version>/clustertemplates/<provider>/<template>.*`
— e.g. `runtime-overlays/7.3.6/clustertemplates/aws/dataengineering-spark3.patch.json`. Providers are
`aws` / `azure` / `gcp` / `yarn`.

## Which versions are overlays

Driven by `cb.runtimes.patched` in `core/src/main/resources/application.yml` (shared with the
blueprint overlay loader). A listed version newer than the base with no on-disk
`defaults/clustertemplates/<version>/` directory is an overlay; everything else is served from disk.
The whole model is gated by `cb.runtimes.overlay.enabled` (default `false`): when off,
`DefaultClusterTemplateCache` skips overlay materialization entirely. The frozen base is
`RuntimeOverlayConstants.BASE_VERSION` (7.3.3), overridable with `cb.runtimes.base` — but only to a
version whose full on-disk `defaults/clustertemplates/<base>/` dir already ships.

> Do not hand-author a concrete version in `/name` or `/distroXTemplate/cluster/blueprintName` in
> overlay additions — write `__RUNTIME_VERSION__` in both and the engine injects the target version.
> (A base file keeps its literal base version, swapped forward per version.) See the module workflow
> in the `cb-new-runtime` skill.

Tests: `RuntimeClusterTemplateOverlayLoaderTest` (real base + `runtime-overlays/7.3.6` fixture) and
`DefaultClusterTemplateCacheTest` (the merge branches).
