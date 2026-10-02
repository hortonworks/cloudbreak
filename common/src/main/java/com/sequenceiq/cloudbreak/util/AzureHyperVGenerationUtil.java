package com.sequenceiq.cloudbreak.util;

import static com.sequenceiq.cloudbreak.constant.AzureConstants.HYPER_V_GENERATION_TAG;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.HYPER_V_GENERATION_V1;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.HYPER_V_GENERATION_V2;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureHyperVGenerationUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureHyperVGenerationUtil.class);

    private AzureHyperVGenerationUtil() {
    }

    /**
     * @return "V1"/"V2" when the image catalog tags declare the Hyper-V generation of an Azure image, empty when they do not. An empty result means
     * "unknown", and callers must not block on it: untagged images keep the pre-existing implicit Gen1 behaviour.
     */
    public static Optional<String> toGeneration(Map<String, String> tags) {
        String tag = tags == null ? null : tags.get(HYPER_V_GENERATION_TAG);
        if (tag == null) {
            return Optional.empty();
        }
        if ("2".equals(tag)) {
            return Optional.of(HYPER_V_GENERATION_V2);
        }
        if ("1".equals(tag)) {
            return Optional.of(HYPER_V_GENERATION_V1);
        }
        LOGGER.warn("Unrecognised '{}' image tag value: '{}' - skipping Hyper-V generation validation.", HYPER_V_GENERATION_TAG, tag);
        return Optional.empty();
    }
}
