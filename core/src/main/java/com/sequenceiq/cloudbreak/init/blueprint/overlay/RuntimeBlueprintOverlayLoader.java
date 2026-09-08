package com.sequenceiq.cloudbreak.init.blueprint.overlay;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.runtime.overlay.RuntimeOverlayConstants;
import com.sequenceiq.cloudbreak.common.runtime.overlay.RuntimeOverlayResolver;
import com.sequenceiq.cloudbreak.init.blueprint.BlueprintEntities;

/**
 * Materializes the {@code core} default blueprints for runtime versions that ship as sparse <em>overlays</em>
 * on top of a frozen base version, instead of a full copy of every {@code .bp} file.
 *
 * <p>The set of overlay versions is declared in {@code application.yml} ({@code cb.runtimes.patched}), the same
 * property that drives the cluster-template overlays. A version is treated as an overlay when it is listed, is
 * newer than the base, and has <em>no</em> on-disk full blueprint directory
 * ({@code defaults/blueprints/<version>/}); everything else is served from disk by
 * {@link com.sequenceiq.cloudbreak.init.blueprint.DefaultBlueprintCache} exactly as before.</p>
 *
 * <p>An overlay's genuine deltas live under {@code classpath:runtime-overlays/<version>/blueprints/} as RFC 6902
 * {@code <stem>.patch.json} files, whole-file {@code <stem>.tombstone} markers, and whole-file {@code <stem>.bp}
 * additions for a blueprint the base never had. Because a blueprint's display name is external to the {@code .bp}
 * file, an addition must ship a companion {@code <stem>.name} sidecar holding the base-version-prefixed display name;
 * this loader reads those sidecars alongside the base YAML block, so a new blueprint is named the same way an
 * existing one is (prefix-swap only).</p>
 *
 * <p>This is a thin, blueprint-specific adapter over the version-agnostic {@link RuntimeOverlayResolver} in
 * {@code common}. Blueprints differ from cluster templates in one important way: the DB identity (the blueprint
 * display name) is <em>not</em> a field inside the {@code .bp} file - it is the left-hand side of the
 * {@code displayName=fileStem} entry in the {@code cb.blueprint.cm.defaults.<version>} YAML block. So this loader
 * reads the base version's YAML block to recover the {@code fileStem -> displayName} mapping, then derives each
 * overlay version's name by swapping only the {@code "<baseVersion> "} prefix of the base display name (never a
 * blind rewrite). The version fields inside the file itself ({@code /description} and {@code /blueprint/cdhVersion})
 * are injected by the shared resolver.</p>
 */
@Component
public class RuntimeBlueprintOverlayLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeBlueprintOverlayLoader.class);

    private static final String BASE_SUBTREE = "defaults/blueprints";

    private static final String OVERLAY_SUBTREE = "blueprints";

    private static final String BLUEPRINT_SUFFIX = ".bp";

    private static final String NAME_SIDECAR_SUFFIX = ".name";

    private static final List<String> VERSION_INJECTION_POINTERS = List.of("/description", "/blueprint/cdhVersion");

    // A flat <stem>.bp file; stems use lower-case letters, digits and hyphens (e.g. cdp-data-engineering-spark3.bp).
    private static final Pattern BLUEPRINT_RELATIVE_PATTERN = Pattern.compile("^[a-z0-9-]+\\.bp$");

    @Inject
    private BlueprintEntities blueprintEntities;

    /**
     * Materializes the blueprints for every patched runtime version that is an overlay (newer than the base and
     * without an on-disk full blueprint directory).
     *
     * @param patchedVersions the overlay runtime versions (from {@code cb.runtimes.patched}); an empty set yields
     *                        no overlays, since overlay versions must be enumerated
     * @return the materialized blueprints; each carries its synthesized display name, its file stem (for the
     *         gov-cloud exclusion filter, which is keyed by stem), and the full {@code .bp} JSON document
     *         (already version-injected on {@code /description} and {@code /blueprint/cdhVersion})
     */
    public List<MaterializedBlueprint> materializeOverlayBlueprints(Set<String> patchedVersions) {
        // A stem's display name comes from the base YAML block for a blueprint that already exists in the base, or from
        // a <stem>.name sidecar for a brand-new blueprint an overlay adds (a stem the base block never registered). The
        // base block wins on a conflict; both values are base-version-prefixed, so the same prefix-swap serves either.
        Map<String, String> stemToName = new LinkedHashMap<>();
        stemToName.putAll(sidecarStemToNameMapping());
        stemToName.putAll(baseStemToNameMapping());
        if (stemToName.isEmpty()) {
            LOGGER.warn("No base blueprint registration or overlay name sidecar found for version {}; skipping blueprint overlay materialization.",
                    RuntimeOverlayConstants.BASE_VERSION);
            return List.of();
        }

        Map<String, Map<String, JsonNode>> overlaysByVersion = RuntimeOverlayResolver.resolveOverlays(
                RuntimeOverlayConstants.BASE_VERSION,
                BASE_SUBTREE,
                OVERLAY_SUBTREE,
                patchedVersions,
                relativePath -> BLUEPRINT_RELATIVE_PATTERN.matcher(relativePath).matches(),
                VERSION_INJECTION_POINTERS,
                BLUEPRINT_SUFFIX);

        List<MaterializedBlueprint> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, JsonNode>> versionEntry : overlaysByVersion.entrySet()) {
            registerMaterialized(result, versionEntry.getKey(), versionEntry.getValue(), stemToName);
        }
        LOGGER.info("Materialized {} overlay blueprint(s) for runtime versions {}.", result.size(), overlaysByVersion.keySet());
        return result;
    }

    private void registerMaterialized(List<MaterializedBlueprint> result, String version, Map<String, JsonNode> materialized,
            Map<String, String> stemToName) {
        String basePrefix = RuntimeOverlayConstants.BASE_VERSION + " ";
        for (Map.Entry<String, JsonNode> entry : materialized.entrySet()) {
            String stem = stripSuffix(entry.getKey());
            String baseName = stemToName.get(stem);
            if (baseName == null) {
                // A .bp with neither a base YAML-block registration nor a <stem>.name sidecar: for a base file this is
                // dead on disk today (not in the live set), and for an overlay addition it means the author forgot the
                // sidecar. Either way there is no DB display name to register it under, so skip it.
                LOGGER.debug("Skipping overlay blueprint file '{}' for version {}: no base registration or name sidecar.", entry.getKey(), version);
                continue;
            }
            if (!baseName.startsWith(basePrefix)) {
                LOGGER.warn("Base blueprint name '{}' does not start with the base version prefix; skipping overlay for version {}.",
                        baseName, version);
                continue;
            }
            String overlayName = version + baseName.substring(RuntimeOverlayConstants.BASE_VERSION.length());
            result.add(new MaterializedBlueprint(overlayName, stem, entry.getValue()));
        }
    }

    /**
     * Recovers the {@code fileStem -> displayName} mapping from the base version's
     * {@code cb.blueprint.cm.defaults.<baseVersion>} YAML block (each entry is {@code displayName=fileStem}).
     */
    private Map<String, String> baseStemToNameMapping() {
        String baseBlock = blueprintEntities.getDefaults().get(RuntimeOverlayConstants.BASE_VERSION);
        Map<String, String> stemToName = new LinkedHashMap<>();
        if (StringUtils.isBlank(baseBlock)) {
            return stemToName;
        }
        for (String rawEntry : baseBlock.split(";")) {
            String entry = rawEntry.trim();
            if (entry.isEmpty()) {
                continue;
            }
            String[] split = entry.split("=", 2);
            if (split.length == 2) {
                stemToName.put(split[1].trim(), split[0].trim());
            }
        }
        return stemToName;
    }

    /**
     * Recovers the {@code fileStem -> displayName} mapping for brand-new blueprints an overlay adds, from
     * {@code <stem>.name} sidecar files under {@code runtime-overlays/<version>/blueprints/}. The name is version-
     * independent (authored once, base-version-prefixed, at the version that introduces the blueprint) because the
     * {@code .bp} addition forward-propagates and the prefix is swapped to the target version at registration; a stem
     * defined by more than one sidecar takes the last-read value, which is harmless since names must stay stable.
     */
    private Map<String, String> sidecarStemToNameMapping() {
        Map<String, String> stemToName = new LinkedHashMap<>();
        try {
            PathMatchingResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = patternResolver.getResources("classpath*:runtime-overlays/**/" + OVERLAY_SUBTREE + "/*" + NAME_SIDECAR_SUFFIX);
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null || !filename.endsWith(NAME_SIDECAR_SUFFIX)) {
                    continue;
                }
                String stem = filename.substring(0, filename.length() - NAME_SIDECAR_SUFFIX.length());
                String name = readSidecarName(resource);
                if (StringUtils.isNotBlank(name)) {
                    stemToName.put(stem, name);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Can't read overlay blueprint name sidecars under runtime-overlays", e);
        }
        return stemToName;
    }

    private String readSidecarName(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    private String stripSuffix(String relativePath) {
        return relativePath.substring(0, relativePath.length() - BLUEPRINT_SUFFIX.length());
    }

    /**
     * A single materialized overlay blueprint: its synthesized DB display name, the base file stem it came from
     * (the key the gov-cloud exclusion filter uses), and the full version-injected {@code .bp} JSON document.
     */
    public record MaterializedBlueprint(String name, String fileStem, JsonNode fileJson) {
    }
}
