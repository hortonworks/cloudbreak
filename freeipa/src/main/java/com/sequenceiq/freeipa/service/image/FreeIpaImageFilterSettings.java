package com.sequenceiq.freeipa.service.image;

import java.util.Map;

import com.sequenceiq.common.model.Architecture;

public record FreeIpaImageFilterSettings(
        String currentImageId,
        String catalog,
        String currentOs,
        String targetOs,
        String region,
        String platform,
        boolean allowMajorOsUpgrade,
        Architecture architecture,
        Map<String, String> tagFilters) {

    public FreeIpaImageFilterSettings(
            String currentImageId,
            String catalog,
            String currentOs,
            String targetOs,
            String region,
            String platform,
            boolean allowMajorOsUpgrade,
            Architecture architecture) {
        this(
                currentImageId,
                catalog,
                currentOs,
                targetOs,
                region,
                platform,
                allowMajorOsUpgrade,
                architecture,
                Map.of()
        );
    }

    /**
     * Creates a copy of the current record with only the currentImageId parameter.
     * All other fields are preserved from the current instance.
     *
     * @param imageId the new currentImageId
     * @return a new FreeIpaImageFilterSettings instance with the updated currentImageId
     */
    public FreeIpaImageFilterSettings withImageId(String imageId) {
        return new FreeIpaImageFilterSettings(
                imageId,
                this.catalog,
                this.currentOs,
                this.targetOs,
                this.region,
                this.platform,
                this.allowMajorOsUpgrade,
                this.architecture,
                this.tagFilters
        );
    }
}
