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
        Map<String, String> tagFilters,
        boolean matchBySourceImageId) {

    public FreeIpaImageFilterSettings(
            String currentImageId,
            String catalog,
            String currentOs,
            String targetOs,
            String region,
            String platform,
            boolean allowMajorOsUpgrade,
            Architecture architecture,
            Map<String, String> tagFilters) {
        this(currentImageId, catalog, currentOs, targetOs, region, platform, allowMajorOsUpgrade, architecture, tagFilters, false);
    }

    public FreeIpaImageFilterSettings(
            String currentImageId,
            String catalog,
            String currentOs,
            String targetOs,
            String region,
            String platform,
            boolean allowMajorOsUpgrade,
            Architecture architecture) {
        this(currentImageId, catalog, currentOs, targetOs, region, platform, allowMajorOsUpgrade, architecture, Map.of(), false);
    }

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
                this.tagFilters,
                this.matchBySourceImageId
        );
    }

    public FreeIpaImageFilterSettings withMatchBySourceImageId(boolean matchBySourceImageId) {
        return new FreeIpaImageFilterSettings(
                this.currentImageId,
                this.catalog,
                this.currentOs,
                this.targetOs,
                this.region,
                this.platform,
                this.allowMajorOsUpgrade,
                this.architecture,
                this.tagFilters,
                matchBySourceImageId
        );
    }
}
