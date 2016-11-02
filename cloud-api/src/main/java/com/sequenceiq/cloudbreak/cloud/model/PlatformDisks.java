package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PlatformDisks {
    private Map<Platform, Collection<DiskType>> diskTypes;

    private Map<Platform, DiskType> defaultDisks;

    private Map<Platform, Map<String, VolumeParameterType>> diskMappings;

    public PlatformDisks(Map<Platform, Collection<DiskType>> diskTypes, Map<Platform, DiskType> defaultDisks,
            Map<Platform, Map<String, VolumeParameterType>> diskMappings) {
        this.diskTypes = diskTypes;
        this.defaultDisks = defaultDisks;
        this.diskMappings = diskMappings;
    }

    public PlatformDisks() {
        this.diskTypes = new HashMap<>();
        this.defaultDisks = new HashMap<>();
        this.diskMappings = new HashMap<>();
    }

    public Map<Platform, Collection<DiskType>> getDiskTypes() {
        return diskTypes;
    }

    public Map<Platform, DiskType> getDefaultDisks() {
        return defaultDisks;
    }

    public Map<Platform, Map<String, VolumeParameterType>> getDiskMappings() {
        return diskMappings;
    }
}
