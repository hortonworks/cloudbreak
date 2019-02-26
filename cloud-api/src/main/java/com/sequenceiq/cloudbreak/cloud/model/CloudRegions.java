package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CloudRegions {

    private final Map<Region, List<AvailabilityZone>> cloudRegions;

    private final Map<Region, String> displayNames;

    private final Map<Region, Coordinate> coordinates;

    private final String defaultRegion;

    private final boolean regionsSupported;

    public CloudRegions(Map<Region, List<AvailabilityZone>> cloudRegions, Map<Region, String> displayNames, Map<Region, Coordinate> coordinates,
            String defaultRegion, boolean regionsSupported) {
        this.cloudRegions = cloudRegions;
        this.displayNames = displayNames;
        this.coordinates = coordinates;
        this.defaultRegion = defaultRegion;
        this.regionsSupported = regionsSupported;
    }

    public Map<Region, List<AvailabilityZone>> getCloudRegions() {
        return new HashMap<>(cloudRegions);
    }

    public Map<Region, String> getDisplayNames() {
        return new HashMap<>(displayNames);
    }

    public String getDefaultRegion() {
        return defaultRegion;
    }

    public Map<Region, Coordinate> getCoordinates() {
        return coordinates;
    }

    public Set<String> getRegionNames() {
        return cloudRegions.keySet().stream()
                .map(Region::getRegionName)
                .collect(Collectors.toSet());
    }

    public String locationNames() {
        return coordinates.keySet()
                .stream()
                .map(Region::getRegionName)
                .collect(Collectors.joining(", "));
    }

    public boolean areRegionsSupported() {
        return regionsSupported;
    }

    @Override
    public String toString() {
        return "CloudRegions{"
                + "cloudRegions=" + cloudRegions
                + ", displayNames=" + displayNames
                + ", coordinates=" + coordinates
                + ", defaultRegion='" + defaultRegion + '\''
                + ", regionsSupported=" + regionsSupported
                + '}';
    }
}
