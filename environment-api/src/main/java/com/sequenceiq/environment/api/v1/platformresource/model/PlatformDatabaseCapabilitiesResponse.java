package com.sequenceiq.environment.api.v1.platformresource.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

public class PlatformDatabaseCapabilitiesResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final Map<String, List<String>> includedRegions;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final Map<String, String> regionDefaultInstances;

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private final Map<String, Map<String, List<String>>> regionUpgradeVersions;

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private final String latestDatabaseEngineVersion;

    public PlatformDatabaseCapabilitiesResponse(Map<String, List<String>> includedRegions, Map<String, String> regionDefaultInstances,
            Map<String, Map<String, List<String>>> regionUpgradeVersions, String latestDatabaseEngineVersion) {
        this.includedRegions = includedRegions;
        this.regionDefaultInstances = regionDefaultInstances;
        this.regionUpgradeVersions = regionUpgradeVersions;
        this.latestDatabaseEngineVersion = latestDatabaseEngineVersion;
    }

    public PlatformDatabaseCapabilitiesResponse(Map<String, List<String>> includedRegions, Map<String, String> regionDefaultInstances,
            String latestDatabaseEngineVersion) {
        this(includedRegions, regionDefaultInstances, new HashMap<>(), latestDatabaseEngineVersion);
    }

    public PlatformDatabaseCapabilitiesResponse() {
        this.regionUpgradeVersions = new HashMap<>();
        this.includedRegions = new HashMap<>();
        this.regionDefaultInstances = new HashMap<>();
        this.latestDatabaseEngineVersion = null;
    }

    public  Map<String, List<String>> getIncludedRegions() {
        return includedRegions;
    }

    public Map<String, String> getRegionDefaultInstances() {
        return regionDefaultInstances;
    }

    public Map<String, Map<String, List<String>>> getRegionUpgradeVersions() {
        return regionUpgradeVersions;
    }

    public String getLatestDatabaseEngineVersion() {
        return latestDatabaseEngineVersion;
    }

    @Override
    public String toString() {
        return "PlatformDatabaseCapabilitiesResponse{" +
                "includedRegions=" + includedRegions +
                ", regionDefaultInstances=" + regionDefaultInstances +
                ", regionUpgradeVersions=" + regionUpgradeVersions +
                ", latestDatabaseEngineVersion='" + latestDatabaseEngineVersion + '\'' +
                '}';
    }
}
