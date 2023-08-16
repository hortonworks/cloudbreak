package com.sequenceiq.environment.api.v1.platformresource.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlatformDatabaseCapabilitiesResponse {

    private final Map<String, List<String>> includedRegions;

    public PlatformDatabaseCapabilitiesResponse(Map<String, List<String>> includedRegions) {
        this.includedRegions = includedRegions;
    }

    public PlatformDatabaseCapabilitiesResponse() {
        this.includedRegions = new HashMap<>();
    }

    public  Map<String, List<String>> getIncludedRegions() {
        return includedRegions;
    }

    @Override
    public String toString() {
        return "PlatformDatabaseCapabilitiesResponse{" +
                "includedRegions=" + includedRegions +
                '}';
    }
}
