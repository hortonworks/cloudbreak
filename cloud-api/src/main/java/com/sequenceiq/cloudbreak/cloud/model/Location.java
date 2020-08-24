package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Location {

    private final Region region;

    private final AvailabilityZone availabilityZone;

    private final Map<String, AvailabilityZone> availabilityZones;

    private Location(Region region, AvailabilityZone availabilityZone, Map<String, AvailabilityZone> availabilityZones) {
        this.region = region;
        this.availabilityZone = availabilityZone;
        this.availabilityZones = availabilityZones;
    }

    public Region getRegion() {
        return region;
    }

    public AvailabilityZone getAvailabilityZone() {
        return availabilityZone;
    }

    public Map<String, AvailabilityZone> getAvailabilityZones() {
        return availabilityZones;
    }

    public static Location location(Region region, AvailabilityZone availabilityZone, Map<String, AvailabilityZone> availabilityZones) {
        return new Location(region, availabilityZone, availabilityZones);
    }

    public static Location location(Region region) {
        return new Location(region, null, new HashMap<>());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        } else if (this == o) {
            return true;
        }
        Location that = (Location) o;
        return Objects.equals(region, that.region)
                && Objects.equals(availabilityZone, that.availabilityZone)
                && Objects.equals(availabilityZones, that.availabilityZones);
    }

    @Override
    public int hashCode() {
        return Objects.hash(region, availabilityZone, availabilityZones);
    }

    @Override
    public String toString() {
        return "Location{" +
                "region=" + region +
                ", availabilityZone=" + availabilityZone +
                '}';
    }
}
