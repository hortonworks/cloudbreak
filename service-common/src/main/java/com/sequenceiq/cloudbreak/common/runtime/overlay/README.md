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
2. Fold in any whole-file **additions** and **replacements** anchored at `≤ V`.
3. Apply every **patch** anchored at `≤ V` in ascending version order — so a change introduced at one
   version forward-propagates into all higher ones. Patches on the same file are concatenated
   (highest anchor wins on a conflicting path). A whole-file delta **resets** its file: patches anchored
   *below* an addition or replacement of that path are dropped rather than replayed against a body they
   were never written for; same-anchor and higher-anchor patches still apply, on top of the new body.
4. Drop any file marked by a **tombstone** anchored at `≤ V` — unless an addition or replacement of that
   path is anchored *above* the tombstone, which revives the file with its new body (the reset in step 3
   covers tombstones too, so a dropped file can be brought back by shipping it whole).
5. Inject V into the caller-named version fields.

## The five overlay flavors

Deltas live under `classpath*:runtime-overlays/<version>/<overlaySubtree>/`:

| file                     | effect                                                                                        |
|--------------------------|-----------------------------------------------------------------------------------------------|
| `<path>.patch.json`      | RFC 6902 patch modifying a base file                                                          |
| `<path>.tombstone`       | empty marker; drops that base file for this version                                           |
| `<path><baseFileSuffix>` | **addition** — a whole new file the base never had (version fields use `__RUNTIME_VERSION__`) |
| `<path>.replace<baseFileSuffix>` | **replacement** — supersedes a base file whole (version fields use `__RUNTIME_VERSION__`) |
| *(nothing)*              | zero-delta: identical to base modulo the injected version string                              |

Additions and replacements forward-propagate (last anchor wins) and compose with patches and tombstones
exactly like base files — a patch can target either, a tombstone can drop either, and either verb anchored
above a tombstone revives the file it dropped.

The two whole-file verbs are mirror images, and each is guarded at resolution time: an **addition** must
*not* collide with a base path (that is a patch or a replacement in disguise), and a **replacement** must
have a base counterpart (otherwise it is an addition). Either mistake fails loud naming the path, rather
than silently overwriting or introducing a file.

### When to use a replacement

A patch is the default verb: it is a reviewable delta, its mandatory `test` ops detect base drift, and
the file keeps inheriting later base changes. A replacement is the **escape hatch for a structural
rewrite** — when the version reorders, regroups or rebuilds a template so thoroughly that the patch is
no longer a delta but an unreviewable wall of guarded `test`+`replace` pairs.

Two questions decide it:

- **Is the patch still a delta a reviewer can read against intent?** If yes, keep it, however long it is.
  A blueprint that moves whole role-config-group sets between host templates yields a ~180-line patch of
  guarded pairs over reordered arrays that nobody can check against intent; a duty that drops one instance
  group and resizes two others yields three to five ops that read exactly like the change.
- **How many files would the replacement multiply into?** A per-provider tree (duties, cluster templates)
  costs one wholesale base copy *per provider* — that many files to hand-fix on every later base change,
  with that many `test` guards lost. A single blueprint costs one.

Note also that the whole file is usually *larger* than the patch, not smaller — 450 lines against 178 for
that blueprint. The win is reviewability, never size.

What it costs, and why it is not the default:

- **no drift detection** — a replacement carries no `test` ops, so a change to the base underneath it
  goes unnoticed instead of failing the build;
- **no future base inheritance** — that file stops tracking the base from its anchor onward, so any
  later base fix must be re-applied by hand (or by a patch anchored above the replacement);
- **no visible delta** — a reviewer sees a whole file, not what changed.

## Version injection

Injection is deliberately field-targeted, never a blind string replace. For each `injectionPointer`,
the leaf is rewritten **only** when it either:

- contains the `__RUNTIME_VERSION__` placeholder (`RuntimeOverlayConstants.RUNTIME_VERSION_PLACEHOLDER`) —
  the form an **addition** authors, e.g. `"__RUNTIME_VERSION__ - Streaming Analytics"` → every occurrence
  is swapped for the target version; or
- starts with `"<baseVersion> "` (e.g. a display name or description) or equals the bare base version
  (e.g. a plain version field) — the form a **base** file carries, e.g. `"7.3.3 - Data Engineering"` →
  only the leading prefix / bare leaf is swapped.

Anything else — a value that merely *contains* the base number, such as a parcel URL or an embedded
component version — is left untouched.

So a base file keeps its literal base version (swapped forward per version), and an addition (a version
newer than the base) writes the placeholder instead of a misleading concrete version. Either way the
caller names the pointers and the engine produces the correctly-versioned value.

## Tests

`RuntimeOverlayResolverTest` and `RuntimeOverlayMaterializerTest` exercise the engine against a
domain-neutral fixture tree (`widgets` / `gadgets`, base `7.0.0`, overlays `7.0.1`–`7.0.3`, plus the
tiny `orphans` / `clashes` trees for the two fail-loud guards) under `src/test/resources/`, covering
chain resolution, forward propagation, and all five flavors. The tree-diff assertions come from
`common`'s `JsonTreeAssertions` test utility.
