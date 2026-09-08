package com.sequenceiq.cloudbreak.init.clustertemplate.overlay;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.runtime.overlay.RuntimeOverlayConstants;
import com.sequenceiq.cloudbreak.common.runtime.overlay.RuntimeOverlayResolver;

/**
 * Materializes the {@code core} default cluster templates for runtime versions that ship as sparse
 * <em>overlays</em> on top of a frozen base version, instead of a full copy of every template file.
 *
 * <p>The set of overlay versions is declared in {@code application.yml} ({@code cb.runtimes.patched}), not
 * in the overlay tree — a version that changes nothing structurally is introduced purely by adding it to
 * that list. A version is treated as an overlay when it is listed, is newer than the base, and has
 * <em>no</em> on-disk full cluster-template directory ({@code defaults/clustertemplates/<version>/});
 * everything else is served from disk by {@link com.sequenceiq.cloudbreak.init.clustertemplate.DefaultClusterTemplateCache}
 * exactly as before.</p>
 *
 * <p>An overlay's genuine deltas live under {@code classpath:runtime-overlays/<version>/clustertemplates/}:</p>
 * <ul>
 *   <li>RFC 6902 patch files {@code <provider>/<template>.patch.json}, and</li>
 *   <li>whole-file tombstones {@code <provider>/<template>.tombstone}.</li>
 * </ul>
 *
 * <p>This is a thin, cluster-template-specific adapter over the version-agnostic
 * {@link RuntimeOverlayResolver} in {@code common}: it supplies the {@code clustertemplates} subtree, the
 * {@code <provider>/<template>.json} file filter, and the two version-carrying pointers ({@code /name} and
 * {@code /distroXTemplate/cluster/blueprintName}), then keys each materialized tree by its {@code /name}
 * and serializes back to a raw JSON string, matching what {@code DefaultClusterTemplateCache} reads from
 * disk — so the cache can merge the two transparently.</p>
 */
@Component
public class RuntimeClusterTemplateOverlayLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeClusterTemplateOverlayLoader.class);

    private static final String BASE_SUBTREE = "defaults/clustertemplates";

    private static final String OVERLAY_SUBTREE = "clustertemplates";

    private static final String NAME_POINTER = "/name";

    private static final String BLUEPRINT_NAME_POINTER = "/distroXTemplate/cluster/blueprintName";

    private static final List<String> VERSION_INJECTION_POINTERS = List.of(NAME_POINTER, BLUEPRINT_NAME_POINTER);

    // A single <provider>/<template>.json segment; providers are aws/azure/gcp/yarn, file names use
    // lower-case letters, digits, hyphens and underscores (e.g. azure/lakehouse_optimizer_ha.json).
    private static final Pattern TEMPLATE_RELATIVE_PATTERN = Pattern.compile("^(aws|azure|gcp|yarn)/[a-z0-9_-]+\\.json$");

    /**
     * Materializes the cluster templates for every patched runtime version that is an overlay (newer than
     * the base and without an on-disk full cluster-template directory).
     *
     * @param patchedVersions the overlay runtime versions (from {@code cb.runtimes.patched}); an empty set
     *                        yields no overlays, since overlay versions must be enumerated
     * @return the materialized cluster templates keyed by their {@code /name}; values are raw JSON strings
     */
    public Map<String, String> materializeOverlayClusterTemplates(Set<String> patchedVersions) {
        Map<String, Map<String, JsonNode>> overlaysByVersion = RuntimeOverlayResolver.resolveOverlays(
                RuntimeOverlayConstants.BASE_VERSION,
                BASE_SUBTREE,
                OVERLAY_SUBTREE,
                patchedVersions,
                relativePath -> TEMPLATE_RELATIVE_PATTERN.matcher(relativePath).matches(),
                VERSION_INJECTION_POINTERS);

        Map<String, String> result = new LinkedHashMap<>();
        try {
            for (Map.Entry<String, Map<String, JsonNode>> versionEntry : overlaysByVersion.entrySet()) {
                registerMaterialized(result, versionEntry.getValue());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Can't serialize materialized runtime overlay cluster templates", e);
        }
        LOGGER.info("Materialized {} overlay cluster template(s) for runtime versions {}.", result.size(), overlaysByVersion.keySet());
        return result;
    }

    private void registerMaterialized(Map<String, String> result, Map<String, JsonNode> materialized) throws IOException {
        for (JsonNode template : materialized.values()) {
            JsonNode name = template.at(NAME_POINTER);
            if (name.isTextual()) {
                result.put(name.asText(), JsonUtil.writeValueAsString(template));
            } else {
                LOGGER.warn("Skipping a materialized overlay cluster template without a textual /name field.");
            }
        }
    }
}
