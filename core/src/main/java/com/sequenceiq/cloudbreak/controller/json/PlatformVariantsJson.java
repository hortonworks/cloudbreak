package com.sequenceiq.cloudbreak.controller.json;

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformVariantsJson implements JsonEntity {

    private Map<String, Collection<String>> platformToVariants;
    private Map<String, String> defaultVariants;

    public Map<String, Collection<String>> getPlatformToVariants() {
        return platformToVariants;
    }

    public void setPlatformToVariants(Map<String, Collection<String>> platformToVariants) {
        this.platformToVariants = platformToVariants;
    }

    public Map<String, String> getDefaultVariants() {
        return defaultVariants;
    }

    public void setDefaultVariants(Map<String, String> defaultVariants) {
        this.defaultVariants = defaultVariants;
    }
}
