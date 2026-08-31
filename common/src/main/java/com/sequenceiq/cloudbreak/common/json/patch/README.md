# JSON Patch (`com.sequenceiq.cloudbreak.common.json.patch`)

A minimal, dependency-free [RFC 6902](https://datatracker.ietf.org/doc/html/rfc6902) JSON Patch
engine over Jackson trees. It applies one patch document to one JSON document — nothing more. It has
no knowledge of any domain that happens to use it.

- `JsonPatchApplier` — apply a patch to a document.
- `JsonPatchLinter` — validate a patch's authoring discipline (below).
- `JsonPatchTestFailedException` — thrown when a `test` op (or a `name=` selector) fails to match.

Everything is static; a patch is never applied in place — the target is deep-copied first.

---

## Applying a patch

```java
JsonNode base   = JsonUtil.readTree(baseJsonString);
JsonNode patch  = JsonUtil.readTree(patchJsonString);   // a JSON array of ops
JsonNode result = JsonPatchApplier.apply(base, patch);  // base is untouched
```

A patch document is a JSON array of operations. Supported ops: **`test`, `replace`, `add`,
`remove`**.

```json
[
  { "op": "test",    "path": "/template/instanceType", "value": "m5.2xlarge" },
  { "op": "replace", "path": "/template/instanceType", "value": "m5.8xlarge" }
]
```

| op        | needs `test` guard? | notes                                                           |
|-----------|---------------------|-----------------------------------------------------------------|
| `test`    | —                   | asserts the value currently at `path`; fails loud on mismatch   |
| `replace` | **yes**             | target must already exist                                       |
| `add`     | no                  | object key, array index, or `-` to append (`/instanceGroups/-`) |
| `remove`  | **yes**             | object key or array element (index or `name=` selector)         |

## The authoring rule (enforced)

**Every `replace` and `remove` must be immediately preceded by a `test` op on the *same path*.**
`JsonPatchApplier.apply` runs `JsonPatchLinter.validate` before applying anything, so an unguarded
mutation is rejected up front with a clear message rather than silently changing a value.

The point is to fail loud if the document a patch was written against has since changed underneath it:
the guarding `test` asserts the expected current value, so drift throws `JsonPatchTestFailedException`
instead of corrupting the result. `add` and `test` need no guard.

## Addressing array elements by field value

As a small, deliberate extension to RFC 6902, a path segment of the form `<field>=<value>` selects
the array element whose `<field>` equals `<value>`, instead of a positional index. Prefer it — it
stays correct when the array is reordered and it reads clearly:

```json
[
  { "op": "test",    "path": "/instanceGroups/name=core/template/instanceType", "value": "m5.2xlarge" },
  { "op": "replace", "path": "/instanceGroups/name=core/template/instanceType", "value": "m5.8xlarge" }
]
```

`/instanceGroups/name=core/...` targets the element whose `name` is `core`. A selector matching no
element fails loud, exactly like a failed `test`. The linter compares paths textually, so a `test`
and the `replace`/`remove` it guards must carry the **identical** selector string.

> Why a hand-rolled applier rather than a maintained RFC 6902 library: the `<field>=<value>` selector
> is not part of RFC 6902, and keeping this small avoids pulling in a third-party dependency for a
> handful of tightly-scoped operations. It is the sanctioned implementation, not a placeholder.

## Tests

`JsonPatchApplierTest` (op mechanics, selector resolution) and `JsonPatchLinterTest` (the guard rule).
