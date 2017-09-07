package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlatformRegions {

    private final Map<Platform, Collection<Region>> regions;

    private final Map<Platform, Map<Region, List<AvailabilityZone>>> availabiltyZones;

    private final Map<Platform, Region> defaultRegions;

    private final Map<Platform, Map<Region, DisplayName>> regionDisplayNames;

    public PlatformRegions(Map<Platform, Collection<Region>> regions, Map<Platform, Map<Region, List<AvailabilityZone>>> availabiltyZones,
            Map<Platform, Region> defaultRegions, Map<Platform, Map<Region, DisplayName>> regionDisplayNames) {
        this.regions = regions;
        this.availabiltyZones = availabiltyZones;
        this.defaultRegions = defaultRegions;
        this.regionDisplayNames = regionDisplayNames;
    }

    public PlatformRegions() {
        regions = new HashMap<>();
        availabiltyZones = new HashMap<>();
        defaultRegions = new HashMap<>();
        regionDisplayNames = new HashMap<>();
    }

    public Map<Platform, Collection<Region>> getRegions() {
        return regions;
    }

    public Map<Platform, Map<Region, List<AvailabilityZone>>> getAvailabiltyZones() {
        return availabiltyZones;
    }

    public Map<Platform, Region> getDefaultRegions() {
        return defaultRegions;
    }

    public Map<Platform, Map<Region, DisplayName>> getRegionDisplayNames() {
        return regionDisplayNames;
    }
}
