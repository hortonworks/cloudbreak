package com.sequenceiq.cloudbreak.controller.json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformRegionsJson implements JsonEntity {

    private Map<String, Map<String, String>> regions;
    private Map<String, Map<String, List<String>>> availabiltyZones;
    private Map<String, String> defaultRegions;

    public PlatformRegionsJson(Map<String, Map<String, String>> regions, Map<String, Map<String, List<String>>> availabiltyZones,
            Map<String, String> defaultRegions) {
        this.regions = regions;
        this.availabiltyZones = availabiltyZones;
        this.defaultRegions = defaultRegions;
    }

    public PlatformRegionsJson() {
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

    public void setRegions(Map<String, Map<String, String>> regions) {
        this.regions = regions;
    }

    public void setAvailabiltyZones(Map<String, Map<String, List<String>>> availabiltyZones) {
        this.availabiltyZones = availabiltyZones;
    }

    public void setDefaultRegions(Map<String, String> defaultRegions) {
        this.defaultRegions = defaultRegions;
    }
}
