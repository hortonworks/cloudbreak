package com.sequenceiq.cloudbreak.cloud.model;

public class AvailabilityZone extends StringType {

    private AvailabilityZone(String value) {
        super(value);
    }

    public static AvailabilityZone availabilityZone(String value) {
        return new AvailabilityZone(value);
    }

}
