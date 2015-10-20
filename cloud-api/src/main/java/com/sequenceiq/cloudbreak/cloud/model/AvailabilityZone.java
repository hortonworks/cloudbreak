package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class AvailabilityZone extends StringType {

    private AvailabilityZone(String value) {
        super(value);
    }

    public static AvailabilityZone availabilityZone(String value) {
        return new AvailabilityZone(value);
    }

}
