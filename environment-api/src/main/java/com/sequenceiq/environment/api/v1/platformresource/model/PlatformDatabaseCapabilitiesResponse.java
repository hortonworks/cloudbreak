package com.sequenceiq.environment.api.v1.platformresource.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlatformDatabaseCapabilitiesResponse {

    private final Map<String, List<String>> includedRegions;

    private final Map<String, String> regionDefaultInstances;

    public PlatformDatabaseCapabilitiesResponse(Map<String, List<String>> includedRegions, Map<String, String> regionDefaultInstances) {
        this.includedRegions = includedRegions;
        this.regionDefaultInstances = regionDefaultInstances;
    }

    public PlatformDatabaseCapabilitiesResponse() {
        this.includedRegions = new HashMap<>();
        this.regionDefaultInstances = new HashMap<>();
    }

    public  Map<String, List<String>> getIncludedRegions() {
        return includedRegions;
    }

    public Map<String, String> getRegionDefaultInstances() {
        return regionDefaultInstances;
    }

    @Override
    public String toString() {
        return "PlatformDatabaseCapabilitiesResponse{" +
                "includedRegions=" + includedRegions +
                ",regionDefaultInstances=" + regionDefaultInstances +
                '}';
    }
}
