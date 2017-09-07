package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;

import com.sequenceiq.cloudbreak.cloud.model.generic.CloudTypes;

public class PlatformImage extends CloudTypes<CustomImage> {

    private final String regex;

    public PlatformImage(Collection<CustomImage> images, String regex) {
        super(images, null);
        this.regex = regex;
    }

    public String getRegex() {
        return regex;
    }
}
