package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AbstractCloudbreakEntity;

@Prototype
public class ImageSettingsEntity extends AbstractCloudbreakEntity<ImageSettingsV4Request, Response, ImageSettingsEntity> {

    public static final String IMAGESETTINGS_REQUEST = "IMAGESETTINGS_REQUEST";

    ImageSettingsEntity(String newId) {
        super(newId);
        setRequest(new ImageSettingsV4Request());
    }

    ImageSettingsEntity() {
        this(IMAGESETTINGS_REQUEST);
    }

    public ImageSettingsEntity(TestContext testContext) {
        super(new ImageSettingsV4Request(), testContext);
    }

    @Override
    public ImageSettingsEntity valid() {
        return this;
    }

    public ImageSettingsEntity withImageCatalog(String imageCatalog) {
        getRequest().setCatalog(imageCatalog);
        return this;
    }

    public ImageSettingsEntity withImageId(String imageId) {
        getRequest().setId(imageId);
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

