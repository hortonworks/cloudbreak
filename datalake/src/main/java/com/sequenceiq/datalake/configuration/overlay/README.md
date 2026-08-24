# Duty runtime overlay (datalake adapter)

`RuntimeDutyOverlayLoader` materializes the Data Lake **duties** templates for runtime versions that
ship as overlays, so `duties/<version>/` need not be copied per CR. It is a thin adapter over the
shared engine `RuntimeOverlayResolver` (in `service-common`, package
`com.sequenceiq.cloudbreak.common.runtime.overlay`) — see that engine's README for the resolution
algorithm and the four overlay flavors. This file documents only what is specific to duties.

## What this adapter supplies

| engine parameter    | value here                                                        |
|---------------------|-------------------------------------------------------------------|
| `baseSubtree`       | `duties`                                                          |
| `overlaySubtree`    | `duties`                                                          |
| file filter         | `^([a-z]+)/([a-z_]+)\.json$` (one `<platform>/<shape>.json`)       |
| injection pointer   | `/cluster/blueprintName`                                          |

Each materialized template is mapped onto a `CDPConfigKey` — parsed from the relative path:
`<platform>` → `CloudPlatform`, `<shape>` → `SdxClusterShape` (a trailing `_ARM` selects
`Architecture.ARM64`, otherwise `X86_64`), plus the runtime version. The value is the raw JSON string
`CDPConfigService` stores for on-disk versions, merged with **on-disk wins**.

The filter is single-segment on purpose — it mirrors `CDPConfigService`'s own matching and excludes
nested duties (e.g. `..._with_profiler/...`), which are not loaded from disk today either.

## Where deltas live

`datalake/src/main/resources/runtime-overlays/<version>/duties/<platform>/<shape>.*` — e.g.
`runtime-overlays/7.3.6/duties/aws/medium_duty_ha.patch.json`.

## Which versions are overlays

Driven by `datalake.runtimes.supported` in `datalake/src/main/resources/application.yml`. A supported
version newer than the base with no on-disk `duties/<version>/` directory is an overlay. (Note this is
a *different* property from the one the core blueprint/cluster-template loaders use — both modules must
list a new version.) The whole model is gated by `cb.runtimes.overlay.enabled` (default `false`): when
off, `CDPConfigService` skips overlay materialization entirely. The frozen base is
`RuntimeOverlayConstants.BASE_VERSION` (7.3.3), overridable with `cb.runtimes.base` — but only to a
version whose full on-disk `duties/<base>/` dir already ships.

> Do not hand-author a concrete version in `/cluster/blueprintName` in overlay additions — write
> `__RUNTIME_VERSION__` and the engine injects the target version. (A base file keeps its literal base
> version, swapped forward per version.) See the module workflow in the `cb-new-runtime` skill.

Tests: `RuntimeDutyOverlayLoaderTest` (real base + `runtime-overlays/7.3.6` fixture) and
`CDPConfigServiceTest` (the merge/surface behavior).
