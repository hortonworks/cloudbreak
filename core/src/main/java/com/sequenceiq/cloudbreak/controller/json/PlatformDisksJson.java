package com.sequenceiq.cloudbreak.controller.json;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformDisksJson implements JsonEntity {

    private Map<String, Collection<String>> diskTypes;
    private Map<String, String> defaultDisks;

    public PlatformDisksJson() {
        this.diskTypes = new HashMap<>();
        this.defaultDisks = new HashMap<>();
    }

    public Map<String, Collection<String>> getDiskTypes() {
        return diskTypes;
    }

    public void setDiskTypes(Map<String, Collection<String>> diskTypes) {
        this.diskTypes = diskTypes;
    }

    public Map<String, String> getDefaultDisks() {
        return defaultDisks;
    }

    public void setDefaultDisks(Map<String, String> defaultDisks) {
        this.defaultDisks = defaultDisks;
    }
}
