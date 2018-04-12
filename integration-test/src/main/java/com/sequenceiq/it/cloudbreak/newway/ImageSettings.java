package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

import java.util.function.Function;

public class ImageSettings extends Entity {
    public static final String IMAGESETTINGS_REQUEST = "IMAGESETTINGS_REQUEST";

    private com.sequenceiq.cloudbreak.api.model.v2.ImageSettings request;

    ImageSettings(String newId) {
        super(newId);
        this.request = new com.sequenceiq.cloudbreak.api.model.v2.ImageSettings();
    }

    ImageSettings() {
        this(IMAGESETTINGS_REQUEST);
    }

    public com.sequenceiq.cloudbreak.api.model.v2.ImageSettings getRequest() {
        return request;
    }

    public void setRequest(com.sequenceiq.cloudbreak.api.model.v2.ImageSettings request) {
        this.request = request;
    }

    public ImageSettings withImageCatalog(String imageCatalog) {
        request.setImageCatalog(imageCatalog);
        return this;
    }

    public ImageSettings withImageId(String imageId) {
        request.setImageId(imageId);
        return this;
    }

    public static Function<IntegrationTestContext, ImageSettings> getTestContextImageSettings(String key) {
        return (testContext) -> testContext.getContextParam(key, ImageSettings.class);
    }

    public static Function<IntegrationTestContext, ImageSettings> getTestContextImageSettings() {
        return getTestContextImageSettings(IMAGESETTINGS_REQUEST);
    }

    public static Function<IntegrationTestContext, ImageSettings> getNewImageSettings() {
        return (testContext) -> new ImageSettings();
    }

    public static ImageSettings request(String key) {
        return new ImageSettings(key);
    }

    public static ImageSettings request() {
        return new ImageSettings();
    }
}

