package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.Map;

public class PlatformVariants {
    private Map<Platform, Collection<Variant>> platformToVariants;
    private Map<Platform, Variant> defaultVariants;

    public PlatformVariants(Map<Platform, Collection<Variant>> platformToVariants, Map<Platform, Variant> defaultVariants) {
        this.platformToVariants = platformToVariants;
        this.defaultVariants = defaultVariants;
    }

    public Map<Platform, Collection<Variant>> getPlatformToVariants() {
        return platformToVariants;
    }

    public Map<Platform, Variant> getDefaultVariants() {
        return defaultVariants;
    }

}
