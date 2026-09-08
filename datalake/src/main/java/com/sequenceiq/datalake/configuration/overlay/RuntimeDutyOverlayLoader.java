package com.sequenceiq.datalake.configuration.overlay;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.runtime.overlay.RuntimeOverlayConstants;
import com.sequenceiq.cloudbreak.common.runtime.overlay.RuntimeOverlayResolver;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.datalake.service.sdx.CDPConfigKey;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

/**
 * Materializes the Data Lake "duties" templates for runtime versions that ship as sparse
 * <em>overlays</em> on top of a frozen base version, instead of a full copy of every duty file.
 *
 * <p>The set of runtime versions is declared in {@code application.yml} ({@code datalake.runtimes.supported}),
 * not in the overlay tree — so a version that changes nothing structurally is introduced purely by adding
 * it to that list. A version is treated as an overlay when it is supported, is newer than the base, and has
 * <em>no</em> on-disk full duty directory ({@code duties/<version>/}); everything else is served from disk
 * by {@code CDPConfigService} exactly as before.</p>
 *
 * <p>An overlay's genuine deltas live under {@code classpath:runtime-overlays/<version>/duties/}:</p>
 * <ul>
 *   <li>RFC 6902 patch files {@code <platform>/<shape>.patch.json}, and</li>
 *   <li>whole-file tombstones {@code <platform>/<shape>.tombstone}.</li>
 * </ul>
 *
 * <p>This is a thin, duty-specific adapter over the version-agnostic {@link RuntimeOverlayResolver} in
 * {@code common}: it supplies the {@code duties} subtree, the single-segment {@code <platform>/<shape>.json}
 * file filter, and the one version-carrying pointer ({@code /cluster/blueprintName}), then maps each
 * materialized relative path onto a {@link CDPConfigKey} and serializes back to the raw JSON string
 * {@code CDPConfigService} stores for on-disk versions — so the loader can merge the two transparently.</p>
 */
@Component
public class RuntimeDutyOverlayLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeDutyOverlayLoader.class);

    private static final String DUTIES_SUBTREE = "duties";

    private static final String ARM_SUFFIX = "_ARM";

    private static final List<String> VERSION_INJECTION_POINTERS = List.of("/cluster/blueprintName");

    // Mirrors CDPConfigService's own shape matching: a single <platform>/<shape>.json segment, where
    // <platform> is one of the providers shipped under the latest (7.3.3) duties tree. This deliberately
    // excludes nested duties such as cdp_data_lake_medium_duty_with_profiler/..., which CDPConfigService
    // does not load today either.
    private static final Pattern DUTY_RELATIVE_PATTERN = Pattern.compile("^(aws|azure|gcp|yarn|mock|openstack)/([a-z0-9_-]+)\\.json$");

    /**
     * Materializes the duty templates for every supported runtime version that is an overlay (newer than
     * the base and without an on-disk full duty directory).
     *
     * @param supportedRuntimes the supported runtime versions (from {@code datalake.runtimes.supported});
     *                          an empty set yields no overlays, since overlay versions must be enumerated
     * @return the materialized duties keyed by {@link CDPConfigKey}; values are raw JSON strings
     */
    public Map<CDPConfigKey, String> materializeOverlayDuties(Set<String> supportedRuntimes) {
        Map<String, Map<String, JsonNode>> overlaysByVersion = RuntimeOverlayResolver.resolveOverlays(
                RuntimeOverlayConstants.BASE_VERSION,
                DUTIES_SUBTREE,
                DUTIES_SUBTREE,
                supportedRuntimes,
                relativePath -> DUTY_RELATIVE_PATTERN.matcher(relativePath).matches(),
                VERSION_INJECTION_POINTERS);

        Map<CDPConfigKey, String> result = new HashMap<>();
        try {
            for (Map.Entry<String, Map<String, JsonNode>> versionEntry : overlaysByVersion.entrySet()) {
                registerMaterialized(result, versionEntry.getValue(), versionEntry.getKey());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Can't serialize materialized runtime overlay duty templates", e);
        }
        LOGGER.info("Materialized {} overlay duty template(s) for runtime versions {}.", result.size(), overlaysByVersion.keySet());
        return result;
    }

    private void registerMaterialized(Map<CDPConfigKey, String> result, Map<String, JsonNode> materialized, String version)
            throws IOException {
        for (Map.Entry<String, JsonNode> entry : materialized.entrySet()) {
            CDPConfigKey key = toConfigKey(entry.getKey(), version);
            if (key != null) {
                result.put(key, JsonUtil.writeValueAsString(entry.getValue()));
            }
        }
    }

    private CDPConfigKey toConfigKey(String relativePath, String version) {
        Matcher matcher = DUTY_RELATIVE_PATTERN.matcher(relativePath);
        if (!matcher.matches()) {
            return null;
        }
        String shapeString = matcher.group(2).toUpperCase(Locale.ROOT);
        Architecture architecture = Architecture.X86_64;
        if (shapeString.contains(ARM_SUFFIX)) {
            shapeString = shapeString.replace(ARM_SUFFIX, "");
            architecture = Architecture.ARM64;
        }
        SdxClusterShape shape = SdxClusterShape.valueOf(shapeString);
        CloudPlatform platform = CloudPlatform.valueOf(matcher.group(1).toUpperCase(Locale.ROOT));
        return new CDPConfigKey(platform, shape, version, architecture);
    }
}
