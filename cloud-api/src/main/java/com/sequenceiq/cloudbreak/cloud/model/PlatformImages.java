package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.Map;

public class PlatformImages {

    private final Map<Platform, Collection<CustomImage>> images;

    private final Map<Platform, String> regex;

    public PlatformImages(Map<Platform, Collection<CustomImage>> images, Map<Platform, String> regex) {
        this.images = images;
        this.regex = regex;
    }

    public Map<Platform, String> getRegex() {
        return regex;
    }

    public Map<Platform, Collection<CustomImage>> getImages() {
        return images;
    }

}
