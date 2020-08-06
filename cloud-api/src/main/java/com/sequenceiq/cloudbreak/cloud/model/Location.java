package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Objects;

public class Location {

    private final Region region;

    private final AvailabilityZone availabilityZone;

    private Location(Region region, AvailabilityZone availabilityZone) {
        this.region = region;
        this.availabilityZone = availabilityZone;
    }

    public Region getRegion() {
        return region;
    }

    public AvailabilityZone getAvailabilityZone() {
        return availabilityZone;
    }

    public static Location location(Region region, AvailabilityZone availabilityZone) {
        return new Location(region, availabilityZone);
    }

    public static Location location(Region region) {
        return new Location(region, null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        } else if (this == o) {
            return true;
        }
        Location that = (Location) o;
        return Objects.equals(region, that.region) && Objects.equals(availabilityZone, that.availabilityZone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(region, availabilityZone);
    }

    @Override
    public String toString() {
        return "Location{" +
                "region=" + region +
                ", availabilityZone=" + availabilityZone +
                '}';
    }
}
