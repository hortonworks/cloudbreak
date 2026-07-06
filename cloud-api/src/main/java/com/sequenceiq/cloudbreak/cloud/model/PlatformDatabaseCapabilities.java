package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlatformDatabaseCapabilities {

    private final Map<DatabaseAvailabiltyType, Collection<Region>> enabledRegions;

    private final Map<Region, String> regionDefaultInstanceTypeMap;

    private final Map<Region, Map<String, List<String>>> supportedServerVersionsToUpgrade;

    private final String latestDatabaseEngineVersion;

    private final Map<Region, Set<DatabaseVmType>> regionAvailableInstanceTypes;

    public PlatformDatabaseCapabilities(
            Map<DatabaseAvailabiltyType, Collection<Region>> enabledRegions,
            Map<Region, String> regionDefaultInstanceTypeMap,
            Map<Region, Map<String, List<String>>> supportedServerVersionsToUpgrade,
            String latestDatabaseEngineVersion) {
        this(enabledRegions, regionDefaultInstanceTypeMap, supportedServerVersionsToUpgrade, latestDatabaseEngineVersion, new HashMap<>());
    }

    public PlatformDatabaseCapabilities(
            Map<DatabaseAvailabiltyType, Collection<Region>> enabledRegions,
            Map<Region, String> regionDefaultInstanceTypeMap,
            Map<Region, Map<String, List<String>>> supportedServerVersionsToUpgrade,
            String latestDatabaseEngineVersion,
            Map<Region, Set<DatabaseVmType>> regionAvailableInstanceTypes) {
        this.enabledRegions = enabledRegions;
        this.regionDefaultInstanceTypeMap = regionDefaultInstanceTypeMap;
        this.supportedServerVersionsToUpgrade = supportedServerVersionsToUpgrade;
        this.latestDatabaseEngineVersion = latestDatabaseEngineVersion;
        this.regionAvailableInstanceTypes = regionAvailableInstanceTypes;
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

    public String getLatestDatabaseEngineVersion() {
        return latestDatabaseEngineVersion;
    }

    public Map<Region, Set<DatabaseVmType>> getRegionAvailableInstanceTypes() {
        return regionAvailableInstanceTypes;
    }
}