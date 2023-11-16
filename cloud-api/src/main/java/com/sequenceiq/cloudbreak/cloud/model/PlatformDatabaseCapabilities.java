package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.Map;

public class PlatformDatabaseCapabilities {

    private final Map<DatabaseAvailabiltyType, Collection<Region>> enabledRegions;

    private final Map<Region, String> regionDefaultInstanceTypeMap;

    public PlatformDatabaseCapabilities(Map<DatabaseAvailabiltyType, Collection<Region>> enabledRegions, Map<Region, String> regionDefaultInstanceTypeMap) {
        this.enabledRegions = enabledRegions;
        this.regionDefaultInstanceTypeMap = regionDefaultInstanceTypeMap;
    }

    public Map<DatabaseAvailabiltyType, Collection<Region>> getEnabledRegions() {
        return enabledRegions;
    }

    public Map<Region, String> getRegionDefaultInstanceTypeMap() {
        return regionDefaultInstanceTypeMap;
    }
}
