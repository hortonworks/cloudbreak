package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PlatformDisks {

    private final Map<Platform, Collection<DiskType>> diskTypes;

    private final Map<Platform, DiskType> defaultDisks;

    private final Map<Platform, Map<String, VolumeParameterType>> diskMappings;

    private final Map<Platform, Map<DiskType, DisplayName>> diskDisplayNames;

    public PlatformDisks(Map<Platform, Collection<DiskType>> diskTypes, Map<Platform, DiskType> defaultDisks,
            Map<Platform, Map<String, VolumeParameterType>> diskMappings, Map<Platform, Map<DiskType, DisplayName>> diskDisplayNames) {
        this.diskTypes = diskTypes;
        this.defaultDisks = defaultDisks;
        this.diskMappings = diskMappings;
        this.diskDisplayNames = diskDisplayNames;
    }

    public PlatformDisks() {
        diskTypes = new HashMap<>();
        defaultDisks = new HashMap<>();
        diskMappings = new HashMap<>();
        diskDisplayNames = new HashMap<>();
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

    public Map<Platform, Map<DiskType, DisplayName>> getDiskDisplayNames() {
        return diskDisplayNames;
    }
}
