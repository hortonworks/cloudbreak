package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlatformRegions {
    private Map<String, Map<String, String>> regions;
    private Map<String, Map<String, List<String>>> availabiltyZones;
    private Map<String, String> defaultRegions;

    public PlatformRegions(Map<String, Map<String, String>> regions, Map<String, Map<String, List<String>>> availabiltyZones,
            Map<String, String> defaultRegions) {
        this.regions = regions;
        this.availabiltyZones = availabiltyZones;
        this.defaultRegions = defaultRegions;
    }

    public PlatformRegions() {
        this.regions = new HashMap<>();
        this.availabiltyZones = new HashMap<>();
        this.defaultRegions = new HashMap<>();
    }

    public Map<String, Map<String, String>> getRegions() {
        return regions;
    }

    public Map<String, Map<String, List<String>>> getAvailabiltyZones() {
        return availabiltyZones;
    }

    public Map<String, String> getDefaultRegions() {
        return defaultRegions;
    }

}
