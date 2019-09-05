package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.v2.ImageSettings;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class ImageSettingsEntity extends AbstractCloudbreakEntity<com.sequenceiq.cloudbreak.api.model.v2.ImageSettings, Response, ImageSettingsEntity,
        Response> {

    public static final String IMAGESETTINGS_REQUEST = "IMAGESETTINGS_REQUEST";

    ImageSettingsEntity(String newId) {
        super(newId);
        setRequest(new com.sequenceiq.cloudbreak.api.model.v2.ImageSettings());
    }

    ImageSettingsEntity() {
        this(IMAGESETTINGS_REQUEST);
    }

    public ImageSettingsEntity(TestContext testContext) {
        super(new ImageSettings(), testContext);
    }

    public ImageSettingsEntity withImageCatalog(String imageCatalog) {
        getRequest().setImageCatalog(imageCatalog);
        return this;
    }

    public ImageSettingsEntity withImageId(String imageId) {
        getRequest().setImageId(imageId);
        return this;
    }

    public ImageSettingsEntity withOs(String os) {
        getRequest().setOs(os);
        return this;
    }

    public static Function<IntegrationTestContext, ImageSettingsEntity> getTestContextImageSettings(String key) {
        return testContext -> testContext.getContextParam(key, ImageSettingsEntity.class);
    }

    public static Function<IntegrationTestContext, ImageSettingsEntity> getTestContextImageSettings() {
        return getTestContextImageSettings(IMAGESETTINGS_REQUEST);
    }

    public static Function<IntegrationTestContext, ImageSettingsEntity> getNewImageSettings() {
        return testContext -> new ImageSettingsEntity();
    }

    public static ImageSettingsEntity request(String key) {
        return new ImageSettingsEntity(key);
    }

    public static ImageSettingsEntity request() {
        return new ImageSettingsEntity();
    }
}

