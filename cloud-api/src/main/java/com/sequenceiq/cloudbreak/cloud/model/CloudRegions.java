package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudRegions {

    private Map<Region, List<AvailabilityZone>> cloudRegions = new HashMap<>();

    private Map<Region, String> displayNames = new HashMap<>();

    private String defaultRegion;

    public CloudRegions() {
    }

    public CloudRegions(Map<Region, List<AvailabilityZone>> cloudRegions, Map<Region, String> displayNames, String defaultRegion) {
        this.cloudRegions = cloudRegions;
        this.displayNames = displayNames;
        this.defaultRegion = defaultRegion;
    }

    public Map<Region, List<AvailabilityZone>> getCloudRegions() {
        return cloudRegions;
    }

    public void setCloudRegions(Map<Region, List<AvailabilityZone>> cloudRegions) {
        this.cloudRegions = cloudRegions;
    }

    public Map<Region, String> getDisplayNames() {
        return displayNames;
    }

    public void setDisplayNames(Map<Region, String> displayNames) {
        this.displayNames = displayNames;
    }

    public String getDefaultRegion() {
        return defaultRegion;
    }

    public void setDefaultRegion(String defaultRegion) {
        this.defaultRegion = defaultRegion;
    }
}
