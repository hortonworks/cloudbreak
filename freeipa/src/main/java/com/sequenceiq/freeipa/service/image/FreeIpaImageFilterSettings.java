package com.sequenceiq.freeipa.service.image;

public record FreeIpaImageFilterSettings(String currentImageId, String catalog, String os, String region, String platform, boolean allowMajorOsUpgrade) {
}
