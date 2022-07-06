package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class Region extends StringType {

    @JsonCreator
    private Region(@JsonProperty("value") String value) {
        super(value);
    }

    public static Region region(String value) {
        return new Region(value);
    }

    public String getRegionName() {
        return Strings.isNullOrEmpty(value()) ? "null" : value();
    }
}
