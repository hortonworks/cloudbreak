package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class Variant extends StringType {

    public static final Variant EMPTY = new Variant("");

    @JsonCreator
    private Variant(@JsonProperty("value") String variant) {
        super(variant);
    }

    public static Variant variant(String variant) {
        return new Variant(variant);
    }
}
