package com.sequenceiq.it.cloudbreak.newway.dto;

import java.util.function.Function;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ImageSettingsTestDto extends AbstractCloudbreakTestDto<ImageSettingsV4Request, Response, ImageSettingsTestDto> {

    public static final String IMAGESETTINGS_REQUEST = "IMAGESETTINGS_REQUEST";

    ImageSettingsTestDto(String newId) {
        super(newId);
        setRequest(new ImageSettingsV4Request());
    }

    ImageSettingsTestDto() {
        this(IMAGESETTINGS_REQUEST);
    }

    public ImageSettingsTestDto(TestContext testContext) {
        super(new ImageSettingsV4Request(), testContext);
    }

    @Override
    public ImageSettingsTestDto valid() {
        return this;
    }

    public ImageSettingsTestDto withImageCatalog(String imageCatalog) {
        getRequest().setCatalog(imageCatalog);
        return this;
    }

    public ImageSettingsTestDto withImageId(String imageId) {
        getRequest().setId(imageId);
        return this;
    }

    public ImageSettingsTestDto withOs(String os) {
        getRequest().setOs(os);
        return this;
    }

    public static Function<IntegrationTestContext, ImageSettingsTestDto> getTestContextImageSettings(String key) {
        return testContext -> testContext.getContextParam(key, ImageSettingsTestDto.class);
    }

    public static Function<IntegrationTestContext, ImageSettingsTestDto> getTestContextImageSettings() {
        return getTestContextImageSettings(IMAGESETTINGS_REQUEST);
    }

    public static Function<IntegrationTestContext, ImageSettingsTestDto> getNewImageSettings() {
        return testContext -> new ImageSettingsTestDto();
    }

    public static ImageSettingsTestDto request(String key) {
        return new ImageSettingsTestDto(key);
    }

    public static ImageSettingsTestDto request() {
        return new ImageSettingsTestDto();
    }
}

