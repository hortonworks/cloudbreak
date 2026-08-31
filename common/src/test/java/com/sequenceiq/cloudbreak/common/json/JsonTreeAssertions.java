package com.sequenceiq.cloudbreak.common.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test-support assertions for comparing two Jackson JSON trees <em>semantically</em> (structure and values,
 * ignoring formatting) with a human-readable, path-level failure message.
 *
 * <p>Motivation: a bare {@code assertEquals(expectedTree, actualTree)} prints both trees in full on
 * mismatch, which is unreadable for the large templates in this repo. These helpers instead report the
 * exact JSON Pointer paths that differ. They are the reviewability net for the runtime base+overlay model
 * ("materialize a version, assert the result"): {@link #differingPaths} pins <em>which</em> fields a patch
 * changed, and {@link #assertEqualsIgnoringPaths} expresses "equal to the base except the version fields".</p>
 *
 * <p>Intentionally tree-agnostic — it carries no knowledge of patches, duties, or version strings. The
 * caller supplies the paths to ignore, so the same util serves duties, cluster templates, and blueprints.</p>
 */
public final class JsonTreeAssertions {

    private static final int MAX_REPORTED_DIFFERENCES = 50;

    private JsonTreeAssertions() {
    }

    /**
     * Returns the RFC 6901 JSON Pointer paths at which {@code expected} and {@code actual} differ — a value
     * or type mismatch at a leaf, or a field/element present in only one of the two trees. An empty list
     * means the trees are semantically equal.
     */
    public static List<String> differingPaths(JsonNode expected, JsonNode actual) {
        return collectDifferences(expected, actual).stream().map(Difference::path).collect(Collectors.toList());
    }

    /**
     * Asserts that the two trees are semantically equal, failing with the list of differing paths (and the
     * expected/actual value at each) rather than dumping both trees in full.
     */
    public static void assertSemanticallyEquals(JsonNode expected, JsonNode actual, String message) {
        List<Difference> differences = collectDifferences(expected, actual);
        if (!differences.isEmpty()) {
            throw new AssertionError(message + System.lineSeparator() + format(differences));
        }
    }

    /**
     * Asserts that the two trees are equal once the given JSON Pointer paths are removed from both. Use it
     * to assert "identical apart from the version-carrying fields" — pass those fields as {@code ignoredPaths}.
     * A path that is absent in a tree is silently skipped.
     */
    public static void assertEqualsIgnoringPaths(JsonNode expected, JsonNode actual, Collection<String> ignoredPaths, String message) {
        JsonNode strippedExpected = stripPaths(expected, ignoredPaths);
        JsonNode strippedActual = stripPaths(actual, ignoredPaths);
        assertSemanticallyEquals(strippedExpected, strippedActual, message);
    }

    private static JsonNode stripPaths(JsonNode tree, Collection<String> ignoredPaths) {
        JsonNode copy = tree.deepCopy();
        for (String path : ignoredPaths) {
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash < 0) {
                continue;
            }
            String parentPath = path.substring(0, lastSlash);
            String leaf = decode(path.substring(lastSlash + 1));
            JsonNode parent = parentPath.isEmpty() ? copy : copy.at(parentPath);
            if (parent instanceof ObjectNode objectParent) {
                objectParent.remove(leaf);
            } else if (parent instanceof ArrayNode arrayParent && !leaf.isEmpty() && leaf.chars().allMatch(Character::isDigit)) {
                arrayParent.remove(Integer.parseInt(leaf));
            }
        }
        return copy;
    }

    private static List<Difference> collectDifferences(JsonNode expected, JsonNode actual) {
        List<Difference> differences = new ArrayList<>();
        walk("", expected, actual, differences);
        return differences;
    }

    private static void walk(String path, JsonNode expected, JsonNode actual, List<Difference> differences) {
        if (expected.isObject() && actual.isObject()) {
            Set<String> fields = new LinkedHashSet<>();
            expected.fieldNames().forEachRemaining(fields::add);
            actual.fieldNames().forEachRemaining(fields::add);
            for (String field : fields) {
                walkChild(path + "/" + encode(field), expected.get(field), actual.get(field), differences);
            }
        } else if (expected.isArray() && actual.isArray()) {
            int max = Math.max(expected.size(), actual.size());
            for (int i = 0; i < max; i++) {
                walkChild(path + "/" + i, expected.get(i), actual.get(i), differences);
            }
        } else if (!expected.equals(actual)) {
            differences.add(new Difference(path.isEmpty() ? "" : path, expected, actual));
        }
    }

    private static void walkChild(String childPath, JsonNode expectedChild, JsonNode actualChild, List<Difference> differences) {
        if (expectedChild == null || actualChild == null) {
            differences.add(new Difference(childPath, expectedChild, actualChild));
        } else {
            walk(childPath, expectedChild, actualChild, differences);
        }
    }

    private static String format(List<Difference> differences) {
        List<String> lines = new ArrayList<>();
        // Sort for stable, readable output regardless of field iteration order.
        for (Difference difference : new TreeSet<>(differences)) {
            if (lines.size() == MAX_REPORTED_DIFFERENCES) {
                lines.add("  ... and " + (differences.size() - MAX_REPORTED_DIFFERENCES) + " more");
                break;
            }
            lines.add("  " + difference);
        }
        return String.join(System.lineSeparator(), lines);
    }

    private static String encode(String field) {
        return field.replace("~", "~0").replace("/", "~1");
    }

    private static String decode(String token) {
        return token.replace("~1", "/").replace("~0", "~");
    }

    private record Difference(String path, JsonNode expected, JsonNode actual) implements Comparable<Difference> {

        @Override
        public String toString() {
            String at = path.isEmpty() ? "(root)" : path;
            if (expected == null) {
                return at + ": only in actual (" + actual + ")";
            }
            if (actual == null) {
                return at + ": only in expected (" + expected + ")";
            }
            return at + ": expected " + expected + " but was " + actual;
        }

        @Override
        public int compareTo(Difference other) {
            return path.compareTo(other.path);
        }
    }
}
