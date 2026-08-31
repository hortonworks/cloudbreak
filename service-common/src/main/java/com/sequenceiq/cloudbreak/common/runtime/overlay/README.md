# Runtime base + overlay engine (`com.sequenceiq.cloudbreak.common.runtime.overlay`)

A version-agnostic engine for reconstructing a set of Cloudera Runtime templates from **one frozen
base version plus sparse overlays**, instead of shipping a full copy of every file for every runtime.
The base is a single policy value, `RuntimeOverlayConstants.BASE_VERSION` — its full templates live on
disk; every runtime newer than it is expressed as a small delta on top and materialized in memory.

The engine carries **no knowledge of any particular template tree**. A caller supplies where the base
lives, where its overlays live, which files participate, and which fields carry the version string;
the engine returns the materialized templates keyed by relative path. It builds on the generic JSON
patch engine in `common` (`com.sequenceiq.cloudbreak.common.json.patch`).

- `RuntimeOverlayResolver` — resolves the overlay chain for every supported version and materializes it.
- `RuntimeOverlayMaterializer` — reconstructs one version's templates from the base (patch + inject).
- `RuntimeOverlayConstants` — the shared frozen base version.

Both classes are `final` with static methods; nothing is written to disk.

---

## What a caller provides

`RuntimeOverlayResolver.resolveOverlays(...)` is the entry point. Its parameters are the whole
contract:

| parameter            | meaning                                                                       |
|----------------------|-------------------------------------------------------------------------------|
| `baseVersion`        | the frozen base whose full files are on the classpath (e.g. `7.3.3`)          |
| `baseSubtree`        | classpath root of the base files (e.g. `defaults/clustertemplates`)           |
| `overlaySubtree`     | the leaf under `runtime-overlays/<version>/` holding this tree's deltas        |
| `supportedVersions`  | versions to consider; an empty set yields no overlays                         |
| `baseFileFilter`     | predicate over base-relative paths — only accepted files participate          |
| `injectionPointers`  | JSON Pointers whose leaf carries the version string to rewrite                 |
| `baseFileSuffix`     | on-disk extension of base files (defaults to `.json`; e.g. `.bp`)              |

It returns `Map<version, Map<relativePath, JsonNode>>`. The caller maps those relative paths onto its
own keys (whatever identity its consumer uses).

A version is treated as an **overlay** when it is in `supportedVersions`, is newer than `baseVersion`,
and has **no** on-disk full directory of its own — otherwise it is left to be served from disk as
before. A version that differs from the base only by its version string ships as a **zero-delta**
overlay: list it and nothing else.

## Resolution algorithm (for a version V > base)

1. Load the base files (filtered).
2. Fold in any whole-file **additions** anchored at `≤ V`.
3. Apply every **patch** anchored at `≤ V` in ascending version order — so a change introduced at one
   version forward-propagates into all higher ones. Patches on the same file are concatenated
   (highest anchor wins on a conflicting path).
4. Drop any file marked by a **tombstone** anchored at `≤ V`.
5. Inject V into the caller-named version fields.

## The four overlay flavors

Deltas live under `classpath*:runtime-overlays/<version>/<overlaySubtree>/`:

| file                       | effect                                                                |
|----------------------------|-----------------------------------------------------------------------|
| `<path>.patch.json`        | RFC 6902 patch modifying a base file                                  |
| `<path>.tombstone`         | empty marker; drops that base file for this version                   |
| `<path><baseFileSuffix>`   | **addition** — a whole new file the base never had                    |
| *(nothing)*                | zero-delta: identical to base modulo the injected version string      |

Additions forward-propagate (last anchor wins) and compose with patches and tombstones exactly like
base files — a patch can target an added file, a tombstone can drop one.

## Version injection

Injection is deliberately field-targeted, never a blind string replace. For each `injectionPointer`,
the leaf is rewritten **only** when it starts with `"<baseVersion> "` (e.g. a display name or
description) or equals the bare base version (e.g. a plain version field). Anything that merely
*contains* the base number — parcel URLs, embedded component versions — is left untouched.

So callers never hand-author version strings in overlay files; they name the pointers and the engine
rewrites them per version.

## Tests

`RuntimeOverlayResolverTest` and `RuntimeOverlayMaterializerTest` exercise the engine against a
domain-neutral fixture tree (`widgets` / `gadgets`, base `7.0.0`, overlays `7.0.1`/`7.0.3`) under
`src/test/resources/`, covering chain resolution, forward propagation, and all four flavors. The
tree-diff assertions come from `common`'s `JsonTreeAssertions` test utility.
