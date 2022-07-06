package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class AvailabilityZone extends StringType {

    @JsonCreator
    public AvailabilityZone(@JsonProperty("value") String value) {
        super(value);
    }

    public static AvailabilityZone availabilityZone(String value) {
        return new AvailabilityZone(value);
    }

}
