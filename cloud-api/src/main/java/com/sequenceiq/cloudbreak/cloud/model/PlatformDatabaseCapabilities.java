package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PlatformDatabaseCapabilities {

    private final Map<DatabaseAvailabiltyType, Collection<Region>> enabledRegions;

    private final Map<Region, String> regionDefaultInstanceTypeMap;

    private final Map<Region, Map<String, List<String>>> supportedServerVersionsToUpgrade;

    public PlatformDatabaseCapabilities(Map<DatabaseAvailabiltyType, Collection<Region>> enabledRegions, Map<Region, String> regionDefaultInstanceTypeMap,
            Map<Region, Map<String, List<String>>> supportedServerVersionsToUpgrade) {
        this.enabledRegions = enabledRegions;
        this.regionDefaultInstanceTypeMap = regionDefaultInstanceTypeMap;
        this.supportedServerVersionsToUpgrade = supportedServerVersionsToUpgrade;
    }

    public Map<DatabaseAvailabiltyType, Collection<Region>> getEnabledRegions() {
        return enabledRegions;
    }

    public Map<Region, String> getRegionDefaultInstanceTypeMap() {
        return regionDefaultInstanceTypeMap;
    }

    public Map<Region, Map<String, List<String>>> getSupportedServerVersionsToUpgrade() {
        return supportedServerVersionsToUpgrade;
    }
}