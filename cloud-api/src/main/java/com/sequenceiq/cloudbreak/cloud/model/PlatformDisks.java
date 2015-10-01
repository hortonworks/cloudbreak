package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

public class PlatformDisks {
    private Map<String, Map<String, String>> diskTypes;
    private Map<String, String> defaultDisks;

    public PlatformDisks(Map<String, Map<String, String>> diskTypes, Map<String, String> defaultDisks) {
        this.diskTypes = diskTypes;
        this.defaultDisks = defaultDisks;
    }

    public PlatformDisks() {
        this.diskTypes = new HashMap<>();
        this.defaultDisks = new HashMap<>();
    }

    public Map<String, Map<String, String>> getDiskTypes() {
        return diskTypes;
    }

    public Map<String, String> getDefaultDisks() {
        return defaultDisks;
    }

}
