package com.sequenceiq.cloudbreak.common.json.patch;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Static structural validator for RFC 6902 patch documents. It enforces the one authoring discipline the
 * runtime base+overlay model relies on: <strong>every {@code replace} and {@code remove} must be
 * immediately preceded by a {@code test} op asserting the same path</strong>.
 *
 * <p>Rationale: overlays are cumulative and forward-propagate, so the base a patch was authored against can
 * drift underneath it as later versions are added. A guarding {@code test} op turns that drift into a loud
 * {@link JsonPatchTestFailedException} at materialization time instead of a silently corrupted template
 * shipped to a customer. This linter rejects a patch that omits the guard up front, before
 * {@link JsonPatchApplier} applies a single op, so the failure names the offending op rather than surfacing
 * as a confusing downstream apply error.</p>
 *
 * <p>{@code add} and {@code test} ops need no preceding guard. Paths are compared as authored (raw strings),
 * so the {@code <field>=<value>} name selector supported by {@link JsonPatchApplier} is matched textually:
 * the {@code test} and the {@code replace}/{@code remove} it guards must carry the identical {@code path}.</p>
 */
public final class JsonPatchLinter {

    private static final String OP = "op";

    private static final String PATH = "path";

    private static final String TEST = "test";

    private JsonPatchLinter() {
    }

    /**
     * Validates that every {@code replace}/{@code remove} op in the patch is immediately preceded by a
     * {@code test} op on the same path.
     *
     * @param patch a JSON array of RFC 6902 operations
     * @throws IllegalArgumentException if the patch is not an array, or a {@code replace}/{@code remove} op
     *                                  is not guarded by a preceding {@code test} on the identical path
     */
    public static void validate(JsonNode patch) {
        if (patch == null || !patch.isArray()) {
            throw new IllegalArgumentException("A JSON Patch document must be an array of operations (RFC 6902).");
        }
        JsonNode previous = null;
        int opIndex = 0;
        for (JsonNode operation : patch) {
            String op = text(operation, OP);
            if ("replace".equals(op) || "remove".equals(op)) {
                requireGuardingTest(previous, operation, opIndex);
            }
            previous = operation;
            opIndex++;
        }
    }

    private static void requireGuardingTest(JsonNode previous, JsonNode operation, int opIndex) {
        String path = text(operation, PATH);
        if (previous == null || !TEST.equals(text(previous, OP))) {
            throw new IllegalArgumentException(prefix(opIndex) + "'" + text(operation, OP) + "' at '" + path
                    + "' must be immediately preceded by a 'test' op on the same path, so base drift fails loud.");
        }
        if (!Objects.equals(path, text(previous, PATH))) {
            throw new IllegalArgumentException(prefix(opIndex) + "'" + text(operation, OP) + "' at '" + path
                    + "' is guarded by a 'test' on a different path '" + text(previous, PATH) + "'; the guard must assert the same path.");
        }
    }

    private static String text(JsonNode operation, String field) {
        JsonNode node = operation == null ? null : operation.get(field);
        return node == null ? null : node.asText(null);
    }

    private static String prefix(int opIndex) {
        return "JSON Patch op[" + opIndex + "]: ";
    }
}
