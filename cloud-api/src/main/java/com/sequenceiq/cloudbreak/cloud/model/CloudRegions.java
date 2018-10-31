package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CloudRegions {

    private Map<Region, List<AvailabilityZone>> cloudRegions = new HashMap<>();

    private Map<Region, String> displayNames = new HashMap<>();

    private Map<Region, Coordinate> coordinates = new HashMap<>();

    private String defaultRegion;

    public CloudRegions() {
    }

    public CloudRegions(Map<Region, List<AvailabilityZone>> cloudRegions, Map<Region, String> displayNames, Map<Region, Coordinate> coordinates,
            String defaultRegion) {
        this.cloudRegions = cloudRegions;
        this.displayNames = displayNames;
        this.coordinates = coordinates;
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

    public Map<Region, Coordinate> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Map<Region, Coordinate> coordinates) {
        this.coordinates = coordinates;
    }

    public String locationNames() {
        return coordinates.keySet()
                .stream()
                .map(Region::getRegionName)
                .collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return "CloudRegions{"
                + "cloudRegions=" + cloudRegions
                + ", displayNames=" + displayNames
                + ", defaultRegion='" + defaultRegion + '\''
                + '}';
    }
}
