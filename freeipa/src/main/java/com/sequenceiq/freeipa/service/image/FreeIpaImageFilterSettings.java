package com.sequenceiq.freeipa.service.image;

import com.sequenceiq.common.model.Architecture;

public record FreeIpaImageFilterSettings(
        String currentImageId,
        String catalog,
        String currentOs,
        String targetOs,
        String region,
        String platform,
        boolean allowMajorOsUpgrade,
        Architecture architecture) {
}
