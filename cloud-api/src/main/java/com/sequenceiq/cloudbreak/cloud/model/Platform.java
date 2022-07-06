package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class Platform extends StringType {

    @JsonCreator
    private Platform(@JsonProperty("value") String platform) {
        super(platform);
    }

    public static Platform platform(String platform) {
        return new Platform(platform);
    }
}
