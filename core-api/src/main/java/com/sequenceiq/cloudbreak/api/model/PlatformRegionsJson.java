package com.sequenceiq.cloudbreak.api.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformRegionsJson implements JsonEntity {

    private Map<String, Collection<String>> regions;
    private Map<String, Map<String, Collection<String>>> availabilityZones;
    private Map<String, String> defaultRegions;

    public PlatformRegionsJson() {
        this.regions = new HashMap<>();
        this.availabilityZones = new HashMap<>();
        this.defaultRegions = new HashMap<>();
    }

    public Map<String, Collection<String>> getRegions() {
        return regions;
    }

    public void setRegions(Map<String, Collection<String>> regions) {
        this.regions = regions;
    }

    public Map<String, Map<String, Collection<String>>> getAvailabilityZones() {
        return availabilityZones;
    }

    public void setAvailabilityZones(Map<String, Map<String, Collection<String>>> availabilityZones) {
        this.availabilityZones = availabilityZones;
    }

    public Map<String, String> getDefaultRegions() {
        return defaultRegions;
    }

    public void setDefaultRegions(Map<String, String> defaultRegions) {
        this.defaultRegions = defaultRegions;
    }
}
