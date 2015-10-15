package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;
import java.util.Map;

/**
 * Availability zones of {@link Region} of a {@link Platform}
 *
 * @see Region,Platform,AvailabilityZone
 */
public class AvailabilityZones {
    private Map<Region, List<AvailabilityZone>> availabiltyZones;

    public AvailabilityZones(Map<Region, List<AvailabilityZone>> availabiltyZones) {
        this.availabiltyZones = availabiltyZones;
    }

    public Map<Region, List<AvailabilityZone>> getAll() {
        return availabiltyZones;
    }

    public List<AvailabilityZone> getAvailabilityZonesByRegion(Region region) {
        return availabiltyZones.get(region);
    }
}
