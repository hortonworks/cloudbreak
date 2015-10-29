package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class Platform extends StringType {

    private Platform(String platform) {
        super(platform);
    }

    public static Platform platform(String platform) {
        return new Platform(platform);
    }
}
