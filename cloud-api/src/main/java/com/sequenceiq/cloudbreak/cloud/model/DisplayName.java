package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class DisplayName extends StringType {

    private DisplayName(String value) {
        super(value);
    }

    public static DisplayName displayName(String value) {
        return new DisplayName(value);
    }

}
