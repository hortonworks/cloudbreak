package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class CustomImage extends StringType {

    private final String image;

    private CustomImage(String region, String image) {
        super(region);
        this.image = image;
    }

    public static CustomImage customImage(String region, String image) {
        return new CustomImage(region, image);
    }

    public String getImage() {
        return image;
    }
}
