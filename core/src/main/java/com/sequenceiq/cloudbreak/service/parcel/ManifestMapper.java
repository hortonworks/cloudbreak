package com.sequenceiq.cloudbreak.service.parcel;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;


/**
 * Maps parcel manifest component names to their canonical service names used in
 * CM blueprint templates.
 * <p>
 * Some parcels advertise component names in their manifest that differ from the
 * service type declared in the blueprint (e.g., the manifest uses {@code semantic_search}
 * while the blueprint references {@code opensearch}). This mapper normalizes those
 * names so that {@link ParcelFilterService} can correctly match parcel-provided
 * services against blueprint-required services.
 * Remap component names before caching so all consumers see normalized names.
 * Asked opensearch team to fix but the already created images has the wrong manifest.
 * <p>
 * The mapping is applied in {@link ManifestRetrieverService} before the manifest is
 * cached, so all downstream consumers see normalized names.
 */
@Service
public class ManifestMapper {

    static final Map<String, String> MAPPER = Map.of(
            "semantic_search", "opensearch"
    );

    /**
     * Returns the canonical service name for the given manifest component name.
     * If the name is not in the mapping or is blank, the original value is returned unchanged.
     *
     * @param key the component name from the parcel manifest
     * @return the mapped canonical name, or the original key if no mapping exists
     */
    public String map(String key) {
        if (StringUtils.isBlank(key)) {
            return key;
        } else {
            return MAPPER.getOrDefault(key, key);
        }
    }
}
