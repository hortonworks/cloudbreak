package com.sequenceiq.cloudbreak.controller.json;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformDisksJson implements JsonEntity {

    private Map<String, Map<String, String>> diskTypes;
    private Map<String, String> defaultDisks;

    public PlatformDisksJson(Map<String, Map<String, String>> diskTypes, Map<String, String> defaultDisks) {
        this.diskTypes = diskTypes;
        this.defaultDisks = defaultDisks;
    }

    public PlatformDisksJson() {
        this.diskTypes = new HashMap<>();
        this.defaultDisks = new HashMap<>();
    }

    public Map<String, Map<String, String>> getDiskTypes() {
        return diskTypes;
    }

    public Map<String, String> getDefaultDisks() {
        return defaultDisks;
    }

    public void setDiskTypes(Map<String, Map<String, String>> diskTypes) {
        this.diskTypes = diskTypes;
    }

    public void setDefaultDisks(Map<String, String> defaultDisks) {
        this.defaultDisks = defaultDisks;
    }
}
