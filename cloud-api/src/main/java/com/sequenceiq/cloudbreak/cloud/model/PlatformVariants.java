package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.Map;

public class PlatformVariants {
    private Map<String, Collection<String>> platformToVariants;
    private Map<String, String> defaultVariants;

    public PlatformVariants(Map<String, Collection<String>> platformToVariants, Map<String, String> defaultVariants) {
        this.platformToVariants = platformToVariants;
        this.defaultVariants = defaultVariants;
    }

    public Map<String, Collection<String>> getPlatformToVariants() {
        return platformToVariants;
    }

    public Map<String, String> getDefaultVariants() {
        return defaultVariants;
    }

}
