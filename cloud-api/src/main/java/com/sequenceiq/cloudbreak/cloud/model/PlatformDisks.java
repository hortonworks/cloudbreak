package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PlatformDisks {
    private Map<Platform, Collection<DiskType>> diskTypes;
    private Map<Platform, DiskType> defaultDisks;

    public PlatformDisks(Map<Platform, Collection<DiskType>> diskTypes, Map<Platform, DiskType> defaultDisks) {
        this.diskTypes = diskTypes;
        this.defaultDisks = defaultDisks;
    }

    public PlatformDisks() {
        this.diskTypes = new HashMap<>();
        this.defaultDisks = new HashMap<>();
    }

    public Map<Platform, Collection<DiskType>> getDiskTypes() {
        return diskTypes;
    }

    public Map<Platform, DiskType> getDefaultDisks() {
        return defaultDisks;
    }

}
