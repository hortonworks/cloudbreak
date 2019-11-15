package com.sequenceiq.environment.configuration;

import java.util.Set;

public class SupportedPlatforms {

    private final Set<String> freeIpa;

    public SupportedPlatforms(String[] freeIpa) {
        this.freeIpa = freeIpa == null ? Set.of() : Set.of(freeIpa);
    }

    public boolean supportedPlatformForFreeIpa(String platform) {
        return freeIpa.contains(platform);
    }
}
