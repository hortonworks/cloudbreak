package com.sequenceiq.cloudbreak.common.json.patch;

import java.util.Objects;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Minimal RFC 6902 (JSON Patch) applier over Jackson trees, supporting the operations the runtime
 * base+overlay model needs: {@code test}, {@code replace}, {@code add}, {@code remove}.
 *
 * <p>It is intentionally small and dependency-free. Adopting a maintained RFC 6902 library was considered
 * and deliberately declined: this applier carries a domain-specific {@code <field>=<value>} array selector
 * (below) that a pure RFC 6902 library does not, and keeping it avoids a new third-party dependency for a
 * few tightly-scoped operations. It is the sanctioned implementation, not a placeholder.</p>
 *
 * <p>Overlay authoring discipline: every {@code replace}/{@code remove} must be immediately preceded by a
 * {@code test} op asserting the value currently in the base, so that drift in the base template fails loud
 * ({@link JsonPatchTestFailedException}) instead of silently corrupting the generated template. This is
 * <em>enforced</em> — {@link #apply} runs {@link JsonPatchLinter#validate} before applying any op, so an
 * unguarded mutation is rejected up front rather than surfacing as a confusing downstream apply error.</p>
 *
 * <p>As a small, deliberate extension to RFC 6902, an array element may be addressed by a field value
 * instead of a positional index: a path segment of the form {@code <field>=<value>} against an array
 * resolves to the element whose {@code <field>} equals {@code <value>} (for example
 * {@code /instanceGroups/name=core/template/instanceType} targets the {@code core} host group). This
 * keeps overlay patches readable and stable against host-group reordering, which a bare index is not.
 * A selector that matches no element fails loud like a failed {@code test} op.</p>
 */
public final class JsonPatchApplier {

    private static final String OP = "op";

    private static final String PATH = "path";

    private static final String VALUE = "value";

    private static final String ARRAY_APPEND_TOKEN = "-";

    private static final char SELECTOR_SEPARATOR = '=';

    private static final String POINTER_SEPARATOR = "/";

    private JsonPatchApplier() {
    }

    /**
     * Applies an RFC 6902 patch document to a deep copy of {@code target}; {@code target} is never mutated.
     *
     * @param target the base document
     * @param patch  a JSON array of patch operations
     * @return the patched document
     */
    public static JsonNode apply(JsonNode target, JsonNode patch) {
        if (patch == null || !patch.isArray()) {
            throw new IllegalArgumentException("A JSON Patch document must be an array of operations (RFC 6902).");
        }
        JsonPatchLinter.validate(patch);
        JsonNode result = target.deepCopy();
        int opIndex = 0;
        for (JsonNode operation : patch) {
            result = applyOperation(result, operation, opIndex);
            opIndex++;
        }
        return result;
    }

    private static JsonNode applyOperation(JsonNode root, JsonNode operation, int opIndex) {
        String op = requiredText(operation, OP, opIndex);
        JsonPointer pointer = JsonPointer.compile(resolveSelectors(root, requiredText(operation, PATH, opIndex), opIndex));
        switch (op) {
            case "test":
                applyTest(root, pointer, requiredValue(operation, opIndex), opIndex);
                return root;
            case "replace":
                return applyReplace(root, pointer, requiredValue(operation, opIndex), opIndex);
            case "add":
                return applyAdd(root, pointer, requiredValue(operation, opIndex), opIndex);
            case "remove":
                return applyRemove(root, pointer, opIndex);
            default:
                throw new IllegalArgumentException(prefix(opIndex)
                        + "unsupported op '" + op + "' (this applier supports test, replace, add, remove).");
        }
    }

    private static void applyTest(JsonNode root, JsonPointer pointer, JsonNode expected, int opIndex) {
        JsonNode actual = root.at(pointer);
        if (actual.isMissingNode()) {
            throw new JsonPatchTestFailedException(prefix(opIndex)
                    + "test failed: path '" + pointer + "' does not exist in the base document.");
        }
        if (!Objects.equals(actual, expected)) {
            throw new JsonPatchTestFailedException(prefix(opIndex) + "test failed at '" + pointer
                    + "': base has " + actual + " but the overlay expected " + expected
                    + ". The base template has drifted underneath this patch.");
        }
    }

    private static JsonNode applyReplace(JsonNode root, JsonPointer pointer, JsonNode value, int opIndex) {
        if (pointer.matches()) {
            return value;
        }
        JsonNode parent = requireContainer(root, pointer.head(), opIndex, "replace");
        JsonPointer leaf = pointer.last();
        if (parent.isObject()) {
            String property = leaf.getMatchingProperty();
            if (!parent.has(property)) {
                throw new IllegalArgumentException(prefix(opIndex) + "replace target '" + pointer + "' does not exist.");
            }
            ((ObjectNode) parent).set(property, value);
        } else {
            ArrayNode array = (ArrayNode) parent;
            int index = requireArrayIndex(leaf, array.size(), false, opIndex, "replace");
            array.set(index, value);
        }
        return root;
    }

    private static JsonNode applyAdd(JsonNode root, JsonPointer pointer, JsonNode value, int opIndex) {
        if (pointer.matches()) {
            return value;
        }
        JsonNode parent = requireContainer(root, pointer.head(), opIndex, "add");
        JsonPointer leaf = pointer.last();
        if (parent.isObject()) {
            ((ObjectNode) parent).set(leaf.getMatchingProperty(), value);
        } else {
            ArrayNode array = (ArrayNode) parent;
            if (ARRAY_APPEND_TOKEN.equals(leaf.getMatchingProperty())) {
                array.add(value);
            } else {
                int index = requireArrayIndex(leaf, array.size(), true, opIndex, "add");
                array.insert(index, value);
            }
        }
        return root;
    }

    private static JsonNode applyRemove(JsonNode root, JsonPointer pointer, int opIndex) {
        if (pointer.matches()) {
            throw new IllegalArgumentException(prefix(opIndex) + "cannot remove the document root.");
        }
        JsonNode parent = requireContainer(root, pointer.head(), opIndex, "remove");
        JsonPointer leaf = pointer.last();
        if (parent.isObject()) {
            String property = leaf.getMatchingProperty();
            if (!parent.has(property)) {
                throw new IllegalArgumentException(prefix(opIndex) + "remove target '" + pointer + "' does not exist.");
            }
            ((ObjectNode) parent).remove(property);
        } else {
            ArrayNode array = (ArrayNode) parent;
            int index = requireArrayIndex(leaf, array.size(), false, opIndex, "remove");
            array.remove(index);
        }
        return root;
    }

    private static JsonNode requireContainer(JsonNode root, JsonPointer parentPointer, int opIndex, String op) {
        JsonNode parent = root.at(parentPointer);
        if (parent.isMissingNode() || !(parent.isObject() || parent.isArray())) {
            throw new IllegalArgumentException(prefix(opIndex) + op + " target parent '" + parentPointer
                    + "' is not an existing object or array.");
        }
        return parent;
    }

    private static int requireArrayIndex(JsonPointer leaf, int size, boolean allowEnd, int opIndex, String op) {
        int index = leaf.getMatchingIndex();
        int upperBound = allowEnd ? size : size - 1;
        if (index < 0 || index > upperBound) {
            throw new IllegalArgumentException(prefix(opIndex) + op + " array index '" + leaf.getMatchingProperty()
                    + "' is out of bounds (size " + size + ").");
        }
        return index;
    }

    private static String requiredText(JsonNode operation, String field, int opIndex) {
        JsonNode node = operation.get(field);
        if (node == null || !node.isTextual()) {
            throw new IllegalArgumentException(prefix(opIndex) + "missing or non-textual '" + field + "'.");
        }
        return node.asText();
    }

    private static JsonNode requiredValue(JsonNode operation, int opIndex) {
        JsonNode node = operation.get(VALUE);
        if (node == null) {
            throw new IllegalArgumentException(prefix(opIndex) + "missing '" + VALUE + "'.");
        }
        return node;
    }

    /**
     * Rewrites {@code <field>=<value>} array selector segments in a JSON Pointer into positional indices,
     * resolving each against the live document so the result is a plain RFC 6902 pointer. Non-selector
     * segments are passed through untouched.
     */
    private static String resolveSelectors(JsonNode root, String rawPath, int opIndex) {
        if (rawPath.isEmpty() || rawPath.indexOf(SELECTOR_SEPARATOR) < 0) {
            return rawPath;
        }
        String[] tokens = rawPath.split(POINTER_SEPARATOR, -1);
        StringBuilder resolved = new StringBuilder();
        JsonNode cursor = root;
        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i];
            String decoded = decode(token);
            int separator = decoded.indexOf(SELECTOR_SEPARATOR);
            if (cursor != null && cursor.isArray() && separator >= 0) {
                String field = decoded.substring(0, separator);
                String value = decoded.substring(separator + 1);
                int index = indexOfMatch((ArrayNode) cursor, field, value);
                if (index < 0) {
                    throw new JsonPatchTestFailedException(prefix(opIndex) + "selector '" + token
                            + "' matched no array element (no element with " + field + "=" + value + ").");
                }
                resolved.append('/').append(index);
                cursor = cursor.get(index);
            } else {
                resolved.append('/').append(token);
                cursor = advance(cursor, decoded);
            }
        }
        return resolved.toString();
    }

    private static int indexOfMatch(ArrayNode array, String field, String value) {
        for (int i = 0; i < array.size(); i++) {
            JsonNode element = array.get(i);
            if (element.isObject() && value.equals(element.path(field).asText(null))) {
                return i;
            }
        }
        return -1;
    }

    private static JsonNode advance(JsonNode cursor, String token) {
        if (cursor == null) {
            return null;
        }
        if (cursor.isObject()) {
            return cursor.get(token);
        }
        if (cursor.isArray() && token.chars().allMatch(Character::isDigit) && !token.isEmpty()) {
            return cursor.get(Integer.parseInt(token));
        }
        return null;
    }

    private static String decode(String token) {
        return token.replace("~1", POINTER_SEPARATOR).replace("~0", "~");
    }

    private static String prefix(int opIndex) {
        return "JSON Patch op[" + opIndex + "]: ";
    }
}
