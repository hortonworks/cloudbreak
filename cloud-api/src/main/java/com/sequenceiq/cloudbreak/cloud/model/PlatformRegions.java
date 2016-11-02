package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlatformRegions {

    private final Map<Platform, Collection<Region>> regions;

    private final Map<Platform, Map<Region, List<AvailabilityZone>>> availabiltyZones;

    private final Map<Platform, Region> defaultRegions;

    public PlatformRegions(Map<Platform, Collection<Region>> regions, Map<Platform, Map<Region, List<AvailabilityZone>>> availabiltyZones,
            Map<Platform, Region> defaultRegions) {
        this.regions = regions;
        this.availabiltyZones = availabiltyZones;
        this.defaultRegions = defaultRegions;
    }

    public PlatformRegions() {
        this.regions = new HashMap<>();
        this.availabiltyZones = new HashMap<>();
        this.defaultRegions = new HashMap<>();
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
}
