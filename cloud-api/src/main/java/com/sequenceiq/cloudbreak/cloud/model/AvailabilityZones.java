package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;

/**
 * Availability zones of {@link Region} of a {@link Platform}
 *
 * @see Region,Platform,AvailabilityZone
 */
public class AvailabilityZones {

    private final Map<Region, List<AvailabilityZone>> availabiltyZones;

    public AvailabilityZones(Map<Region, List<AvailabilityZone>> availabiltyZones) {
        this.availabiltyZones = availabiltyZones;
    }

    public Map<Region, List<AvailabilityZone>> getAll() {
        return availabiltyZones;
    }

    public List<AvailabilityZone> getAllAvailabilityZone() {
        List<AvailabilityZone> result = Lists.newArrayList();
        for (Entry<Region, List<AvailabilityZone>> entry : availabiltyZones.entrySet()) {
            result.addAll(entry.getValue());
        }
        return result;
    }

    public List<AvailabilityZone> getAvailabilityZonesByRegion(Region region) {
        return availabiltyZones.get(region);
    }
}
