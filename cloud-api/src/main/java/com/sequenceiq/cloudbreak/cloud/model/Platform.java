package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class Platform extends StringType {

    private static final String NONE = "none";

    private Platform(String platform) {
        super(platform);
    }

    public static Platform platform(String platform) {
        return new Platform(platform);
    }

    public static String toString(Platform platform) {
        return platform != null ? platform.value() : NONE;
    }
}
