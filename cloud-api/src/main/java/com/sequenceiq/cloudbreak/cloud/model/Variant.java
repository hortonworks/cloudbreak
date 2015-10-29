package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class Variant extends StringType {

    public static final Variant EMPTY = new Variant("");

    private Variant(String variant) {
        super(variant);
    }

    public static Variant variant(String variant) {
        return new Variant(variant);
    }
}
