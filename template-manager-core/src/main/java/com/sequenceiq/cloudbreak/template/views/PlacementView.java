package com.sequenceiq.cloudbreak.template.views;

import java.util.Map;

public class PlacementView {

    private final String region;

    private final Map<String, String> availabilityZone;

    public PlacementView(String region, Map<String, String> availabilityZone) {
        this.availabilityZone = availabilityZone;
        this.region = region;
    }

    public Map<String, String> getAvailabilityZone() {
        return availabilityZone;
    }

    public String getRegion() {
        return region;
    }
}
