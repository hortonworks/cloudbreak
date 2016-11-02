package com.sequenceiq.cloudbreak.cloud.model;

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
}
