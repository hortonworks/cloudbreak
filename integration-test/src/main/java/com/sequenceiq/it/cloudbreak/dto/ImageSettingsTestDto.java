package com.sequenceiq.it.cloudbreak.dto;

import java.util.UUID;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;

@Prototype
public class ImageSettingsTestDto extends AbstractCloudbreakTestDto<ImageSettingsV4Request, Response, ImageSettingsTestDto> {

    private static final String DEFAULT_SETTING_NAME = "test-image-setting" + '-' + UUID.randomUUID().toString().replaceAll("-", "");

    public ImageSettingsTestDto(TestContext testContext) {
        super(new ImageSettingsV4Request(), testContext);
    }

    @Override
    public ImageSettingsTestDto valid() {
        return getCloudProvider().imageSettings(withName(getResourcePropertyProvider().getName(getCloudPlatform())));
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

    public ImageSettingsTestDto withName(String name) {
        setName(name);
        return this;
    }

    @Override
    public String getName() {
        return super.getName() == null ? DEFAULT_SETTING_NAME : super.getName();
    }
}

