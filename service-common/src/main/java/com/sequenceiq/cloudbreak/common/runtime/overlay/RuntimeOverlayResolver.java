package com.sequenceiq.cloudbreak.common.runtime.overlay;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;

/**
 * Version-agnostic resolver for the runtime base+overlay model. Given a frozen base version and the set of
 * supported runtime versions, it materializes the template set for every version that ships as a sparse
 * <em>overlay</em> (newer than the base, with no on-disk full directory), reusing
 * {@link RuntimeOverlayMaterializer}.
 *
 * <p>It is intentionally free of any per-tree domain knowledge — the caller passes the {@code baseSubtree}
 * (where the frozen base lives on the classpath, for example {@code duties} or
 * {@code defaults/clustertemplates}), the {@code overlaySubtree} (the leaf under
 * {@code runtime-overlays/<version>/}, for example {@code duties} or {@code clustertemplates}), a filter
 * over base file relative paths, and the JSON Pointers that carry the version prefix. The duty and
 * cluster-template loaders are thin adapters that supply those parameters and then map the returned
 * relative paths onto their own keys.</p>
 *
 * <p>Resolution for a target version {@code V}: start from the base templates, apply every overlay anchored
 * at {@code <= V} in ascending version order (so a change introduced at 7.3.4 forward-propagates into 7.3.5),
 * concatenating same-file patch op arrays (highest-anchor-wins on a conflicting path), then inject the {@code V}
 * version fields. Overlay deltas live under {@code classpath*:runtime-overlays/<version>/<subtree>/} in three
 * kinds: RFC 6902 {@code <path>.patch.json} files that modify a base file, whole-file {@code <path>.tombstone}
 * markers that drop a base file, and whole-file <em>additions</em> — a template that has no counterpart in the base,
 * shipped with the plain base file suffix ({@code <path>.json} / {@code <path>.bp}). Additions forward-propagate
 * (last-anchor-wins on the same path) and compose with patches and tombstones just like base files do.</p>
 */
public final class RuntimeOverlayResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeOverlayResolver.class);

    private static final VersionComparator VERSION_COMPARATOR = new VersionComparator();

    private static final String OVERLAY_ROOT = "runtime-overlays";

    private static final String PATCH_SUFFIX = ".patch.json";

    private static final String TOMBSTONE_SUFFIX = ".tombstone";

    private static final String JSON_SUFFIX = ".json";

    private RuntimeOverlayResolver() {
    }

    /**
     * Materializes the templates for every supported runtime version that is an overlay of the given base,
     * for base trees whose files are {@code .json}. Convenience overload of
     * {@link #resolveOverlays(String, String, String, Set, Predicate, List, String)}.
     */
    public static Map<String, Map<String, JsonNode>> resolveOverlays(
            String baseVersion,
            String baseSubtree,
            String overlaySubtree,
            Set<String> supportedVersions,
            Predicate<String> baseFileFilter,
            List<String> injectionPointers) {
        return resolveOverlays(baseVersion, baseSubtree, overlaySubtree, supportedVersions, baseFileFilter, injectionPointers, JSON_SUFFIX);
    }

    /**
     * Materializes the templates for every supported runtime version that is an overlay of the given base.
     *
     * @param baseVersion       the frozen base version whose full directory lives on disk (for example {@code 7.3.3})
     * @param baseSubtree       the classpath subtree of the frozen base (for example {@code duties} or {@code defaults/clustertemplates})
     * @param overlaySubtree    the leaf under {@code runtime-overlays/<version>/} (for example {@code duties} or {@code clustertemplates})
     * @param supportedVersions the runtime versions to consider; an empty set yields no overlays
     * @param baseFileFilter    accepts the base-relative paths that participate (excludes anything the loader ignores)
     * @param injectionPointers JSON Pointers whose leaf value carries the {@code "<version> - "} prefix to rewrite
     * @param baseFileSuffix    the on-disk extension of the base files (for example {@code .json} for cluster templates,
     *                          {@code .bp} for blueprints); patch files are always {@code .patch.json} and tombstones
     *                          {@code .tombstone} regardless
     * @return materialized templates per overlay version, keyed by version then by base-relative path
     */
    public static Map<String, Map<String, JsonNode>> resolveOverlays(
            String baseVersion,
            String baseSubtree,
            String overlaySubtree,
            Set<String> supportedVersions,
            Predicate<String> baseFileFilter,
            List<String> injectionPointers,
            String baseFileSuffix) {

        if (supportedVersions == null || supportedVersions.isEmpty()) {
            LOGGER.debug("No supported runtime versions supplied; nothing to materialize as an overlay for subtree {}.", overlaySubtree);
            return Map.of();
        }
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Map<String, JsonNode> baseTemplates = loadBaseTemplates(resolver, baseSubtree, baseVersion, baseFileFilter, baseFileSuffix);
            if (baseTemplates.isEmpty()) {
                LOGGER.warn("No base {} templates found for base version {}; skipping overlay materialization.", baseSubtree, baseVersion);
                return Map.of();
            }
            List<String> overlayVersions = supportedVersions.stream()
                    .filter(version -> VERSION_COMPARATOR.compare(versioned(version), versioned(baseVersion)) > 0)
                    .filter(version -> !hasOnDiskDirectory(resolver, baseSubtree, version, baseFileSuffix))
                    .sorted((left, right) -> VERSION_COMPARATOR.compare(versioned(left), versioned(right)))
                    .toList();

            Map<String, VersionOverlay> overlaysByVersion = new LinkedHashMap<>();
            for (String version : overlayVersions) {
                overlaysByVersion.put(version, new VersionOverlay(version,
                        loadPatches(resolver, overlaySubtree, version, baseFileSuffix),
                        loadTombstones(resolver, overlaySubtree, version, baseFileSuffix),
                        loadAdditions(resolver, overlaySubtree, version, baseFileSuffix)));
            }

            Map<String, Map<String, JsonNode>> result = new LinkedHashMap<>();
            for (String version : overlayVersions) {
                Chain chain = resolveChain(version, overlaysByVersion.values());
                // Fold whole-file additions into the base map so materialize emits them alongside base files; a patch
                // keyed on an added path then applies, and a tombstone can drop an added path, exactly as for base files.
                // An addition is by definition a template with no base counterpart, so a clash with a base path is an
                // authoring error (the intent was a patch, not an addition) — fail loud rather than silently overwrite.
                Set<String> clashingAdditions = new HashSet<>(chain.additions().keySet());
                clashingAdditions.retainAll(baseTemplates.keySet());
                if (!clashingAdditions.isEmpty()) {
                    throw new IllegalStateException("Runtime overlay additions for version " + version + " in subtree " + overlaySubtree
                            + " clash with base templates (use a .patch.json instead of a whole-file addition): " + clashingAdditions);
                }
                Map<String, JsonNode> effectiveBase = new LinkedHashMap<>(baseTemplates);
                effectiveBase.putAll(chain.additions());
                result.put(version, RuntimeOverlayMaterializer.materialize(baseVersion, version, effectiveBase, chain.patches(), chain.tombstones(),
                        injectionPointers));
            }
            LOGGER.info("Materialized {} overlay {} version(s): {}.", result.size(), overlaySubtree, overlayVersions);
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Can't materialize runtime overlay templates for subtree " + overlaySubtree, e);
        }
    }

    private static Map<String, JsonNode> loadBaseTemplates(PathMatchingResourcePatternResolver resolver, String baseSubtree, String baseVersion,
            Predicate<String> baseFileFilter, String baseFileSuffix) throws IOException {
        Resource[] resources = resolver.getResources("classpath*:" + baseSubtree + "/" + baseVersion + "/**/*" + baseFileSuffix);
        String marker = "/" + baseSubtree + "/" + baseVersion + "/";
        Map<String, JsonNode> byRelativePath = new LinkedHashMap<>();
        for (Resource resource : resources) {
            String relativePath = relativePathAfter(resource.getURL().getPath(), marker);
            if (relativePath != null && baseFileFilter.test(relativePath)) {
                byRelativePath.put(relativePath, readJson(resource));
            }
        }
        return byRelativePath;
    }

    private static boolean hasOnDiskDirectory(PathMatchingResourcePatternResolver resolver, String subtree, String version, String baseFileSuffix) {
        try {
            // classpath*: returns an empty array (rather than throwing) when the version directory is absent.
            return resolver.getResources("classpath*:" + subtree + "/" + version + "/**/*" + baseFileSuffix).length > 0;
        } catch (IOException e) {
            LOGGER.warn("Failed to probe for an on-disk directory of subtree {} version {}; treating it as an overlay.", subtree, version, e);
            return false;
        }
    }

    private static Map<String, JsonNode> loadPatches(PathMatchingResourcePatternResolver resolver, String subtree, String version, String baseFileSuffix)
            throws IOException {
        Resource[] resources = resolver.getResources("classpath*:" + OVERLAY_ROOT + "/" + version + "/" + subtree + "/**/*" + PATCH_SUFFIX);
        String marker = "/" + version + "/" + subtree + "/";
        Map<String, JsonNode> patches = new LinkedHashMap<>();
        for (Resource resource : resources) {
            String relativePath = relativePathAfter(resource.getURL().getPath(), marker);
            if (relativePath != null) {
                patches.put(stripSuffix(relativePath, PATCH_SUFFIX) + baseFileSuffix, readJson(resource));
            }
        }
        return patches;
    }

    /**
     * Loads whole-file additions: templates a version introduces that have no counterpart in the base. They live
     * alongside patches under {@code runtime-overlays/<version>/<subtree>/} but carry the plain base file suffix
     * (for example {@code <path>.json} or {@code <path>.bp}), so their key already matches a base-relative path and
     * needs no reconstruction. A {@code .patch.json} file also ends in {@code .json}, so when the base suffix is
     * {@code .json} it is filtered out here (it is picked up by {@link #loadPatches} instead); tombstones end in
     * {@code .tombstone} and match neither glob.
     */
    private static Map<String, JsonNode> loadAdditions(PathMatchingResourcePatternResolver resolver, String subtree, String version, String baseFileSuffix)
            throws IOException {
        Resource[] resources = resolver.getResources("classpath*:" + OVERLAY_ROOT + "/" + version + "/" + subtree + "/**/*" + baseFileSuffix);
        String marker = "/" + version + "/" + subtree + "/";
        Map<String, JsonNode> additions = new LinkedHashMap<>();
        for (Resource resource : resources) {
            String relativePath = relativePathAfter(resource.getURL().getPath(), marker);
            if (relativePath != null && !relativePath.endsWith(PATCH_SUFFIX)) {
                additions.put(relativePath, readJson(resource));
            }
        }
        return additions;
    }

    private static Set<String> loadTombstones(PathMatchingResourcePatternResolver resolver, String subtree, String version, String baseFileSuffix)
            throws IOException {
        Resource[] resources = resolver.getResources("classpath*:" + OVERLAY_ROOT + "/" + version + "/" + subtree + "/**/*" + TOMBSTONE_SUFFIX);
        String marker = "/" + version + "/" + subtree + "/";
        Set<String> tombstones = new HashSet<>();
        for (Resource resource : resources) {
            String relativePath = relativePathAfter(resource.getURL().getPath(), marker);
            if (relativePath != null) {
                tombstones.add(stripSuffix(relativePath, TOMBSTONE_SUFFIX) + baseFileSuffix);
            }
        }
        return tombstones;
    }

    /**
     * Resolves the effective patch/tombstone set for {@code targetVersion}: every overlay anchored at
     * {@code <= targetVersion}, applied in ascending version order. Patches touching the same file are
     * concatenated (ascending), so later versions' ops run after earlier ones (highest-anchor-wins on a
     * conflicting path).
     */
    private static Chain resolveChain(String targetVersion, Collection<VersionOverlay> overlays) {
        List<VersionOverlay> applicable = overlays.stream()
                .filter(overlay -> VERSION_COMPARATOR.compare(versioned(overlay.version()), versioned(targetVersion)) <= 0)
                .sorted((left, right) -> VERSION_COMPARATOR.compare(versioned(left.version()), versioned(right.version())))
                .toList();
        Map<String, JsonNode> mergedPatches = new LinkedHashMap<>();
        Set<String> tombstones = new HashSet<>();
        Map<String, JsonNode> additions = new LinkedHashMap<>();
        for (VersionOverlay overlay : applicable) {
            overlay.patches().forEach((path, patch) -> mergePatch(mergedPatches, path, patch));
            tombstones.addAll(overlay.tombstones());
            // A whole-file addition is last-anchor-wins: a later version re-adding the same path replaces it, and an
            // addition introduced at version X forward-propagates into every higher version.
            additions.putAll(overlay.additions());
        }
        return new Chain(mergedPatches, tombstones, additions);
    }

    private static void mergePatch(Map<String, JsonNode> mergedPatches, String path, JsonNode patch) {
        ArrayNode existing = (ArrayNode) mergedPatches.get(path);
        if (existing == null) {
            mergedPatches.put(path, patch.deepCopy());
        } else {
            existing.addAll((ArrayNode) patch);
        }
    }

    private static Versioned versioned(String version) {
        return () -> version;
    }

    private static String relativePathAfter(String fullPath, String marker) {
        int index = fullPath.indexOf(marker);
        return index < 0 ? null : fullPath.substring(index + marker.length());
    }

    private static String stripSuffix(String value, String suffix) {
        return value.substring(0, value.length() - suffix.length());
    }

    private static JsonNode readJson(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return JsonUtil.readTree(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private record VersionOverlay(String version, Map<String, JsonNode> patches, Set<String> tombstones, Map<String, JsonNode> additions) {
    }

    private record Chain(Map<String, JsonNode> patches, Set<String> tombstones, Map<String, JsonNode> additions) {
    }
}
