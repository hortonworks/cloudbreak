package com.sequenceiq.cloudbreak.controller.json;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformRegionsJson implements JsonEntity {

    private Map<String, Collection<String>> regions;
    private Map<String, Map<String, Collection<String>>> availabiltyZones;
    private Map<String, String> defaultRegions;

    public PlatformRegionsJson() {
        this.regions = new HashMap<>();
        this.availabiltyZones = new HashMap<>();
        this.defaultRegions = new HashMap<>();
    }

    public Map<String, Collection<String>> getRegions() {
        return regions;
    }

    public void setRegions(Map<String, Collection<String>> regions) {
        this.regions = regions;
    }

    public Map<String, Map<String, Collection<String>>> getAvailabiltyZones() {
        return availabiltyZones;
    }

    public void setAvailabiltyZones(Map<String, Map<String, Collection<String>>> availabiltyZones) {
        this.availabiltyZones = availabiltyZones;
    }

    public Map<String, String> getDefaultRegions() {
        return defaultRegions;
    }

    public void setDefaultRegions(Map<String, String> defaultRegions) {
        this.defaultRegions = defaultRegions;
    }
}
