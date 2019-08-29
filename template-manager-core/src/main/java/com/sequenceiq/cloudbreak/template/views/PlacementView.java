package com.sequenceiq.cloudbreak.template.views;

public class PlacementView {

    private final String region;

    private final String availabilityZone;

    public PlacementView(String region, String availabilityZone) {
        this.availabilityZone = availabilityZone;
        this.region = region;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public String getRegion() {
        return region;
    }
}
