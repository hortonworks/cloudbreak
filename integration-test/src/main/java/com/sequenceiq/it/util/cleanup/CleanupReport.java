package com.sequenceiq.it.util.cleanup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Per-run cleanup accumulator. One instance is created inside {@link CleanupUtil#cleanupAllResources()}
 * and passed down through the delete methods, so no mutable state survives across calls or threads —
 * this is what replaced the previous {@code @Component}-scoped {@code MultiValueMap} that leaked
 * state between the four sequential cleanup passes.
 * <p>
 * The three maps are keyed by resource type ("distroxName", "sdxName", "credentialName",
 * "environmentName"). {@code TreeMap} keeps the end-of-run summary alphabetical by type;
 * {@code LinkedHashMap} inside {@code deleteErrors} preserves the order in which delete failures
 * were reported for a single type.
 */
class CleanupReport {

    private final Map<String, List<String>> deletedByType = new TreeMap<>();

    private final Map<String, List<String>> leftoversByType = new TreeMap<>();

    private final Map<String, Map<String, String>> deleteErrorsByType = new TreeMap<>();

    void recordDeleted(String resourceType, Collection<String> names) {
        if (names == null || names.isEmpty()) {
            return;
        }
        deletedByType.computeIfAbsent(resourceType, k -> new ArrayList<>()).addAll(names);
    }

    void recordDeleted(String resourceType, String name) {
        deletedByType.computeIfAbsent(resourceType, k -> new ArrayList<>()).add(name);
    }

    void recordLeftovers(String resourceType, Collection<String> names) {
        if (names == null || names.isEmpty()) {
            return;
        }
        leftoversByType.computeIfAbsent(resourceType, k -> new ArrayList<>()).addAll(names);
    }

    void recordDeleteErrors(String resourceType, Map<String, String> errorsByName) {
        if (errorsByName == null || errorsByName.isEmpty()) {
            return;
        }
        deleteErrorsByType.computeIfAbsent(resourceType, k -> new LinkedHashMap<>()).putAll(errorsByName);
    }

    Map<String, List<String>> getDeletedByType() {
        return Collections.unmodifiableMap(deletedByType);
    }

    Map<String, List<String>> getLeftoversByType() {
        return Collections.unmodifiableMap(leftoversByType);
    }

    Map<String, Map<String, String>> getDeleteErrorsByType() {
        return Collections.unmodifiableMap(deleteErrorsByType);
    }

    int deletedCount() {
        return deletedByType.values().stream().mapToInt(List::size).sum();
    }

    int leftoverCount() {
        return leftoversByType.values().stream().mapToInt(List::size).sum();
    }

    int errorCount() {
        return deleteErrorsByType.values().stream().mapToInt(Map::size).sum();
    }

    boolean isEmpty() {
        return deletedByType.isEmpty() && leftoversByType.isEmpty() && deleteErrorsByType.isEmpty();
    }
}
