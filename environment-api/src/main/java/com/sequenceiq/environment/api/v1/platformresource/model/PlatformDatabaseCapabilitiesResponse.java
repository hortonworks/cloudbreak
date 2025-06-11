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

    public PlatformDatabaseCapabilitiesResponse(Map<String, List<String>> includedRegions, Map<String, String> regionDefaultInstances,
            Map<String, Map<String, List<String>>> regionUpgradeVersions) {
        this.includedRegions = includedRegions;
        this.regionDefaultInstances = regionDefaultInstances;
        this.regionUpgradeVersions = regionUpgradeVersions;
    }

    public PlatformDatabaseCapabilitiesResponse(Map<String, List<String>> includedRegions, Map<String, String> regionDefaultInstances) {
        this(includedRegions, regionDefaultInstances, new HashMap<>());
    }

    public PlatformDatabaseCapabilitiesResponse() {
        this.regionUpgradeVersions = new HashMap<>();
        this.includedRegions = new HashMap<>();
        this.regionDefaultInstances = new HashMap<>();
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

    @Override
    public String toString() {
        return "PlatformDatabaseCapabilitiesResponse{" +
                "includedRegions=" + includedRegions +
                ", regionDefaultInstances=" + regionDefaultInstances +
                ", regionUpgradeVersions=" + regionUpgradeVersions +
                '}';
    }
}
