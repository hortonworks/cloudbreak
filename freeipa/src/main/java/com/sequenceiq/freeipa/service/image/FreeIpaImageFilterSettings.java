package com.sequenceiq.freeipa.service.image;

public record FreeIpaImageFilterSettings(
        String currentImageId,
        String catalog,
        String currentOs,
        String targetOs,
        String region,
        String platform,
        boolean allowMajorOsUpgrade) {
}
