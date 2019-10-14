package com.sequenceiq.it.cloudbreak.dto.distrox.image;

import java.util.UUID;

import javax.ws.rs.core.Response;

import com.sequenceiq.distrox.api.v1.distrox.model.image.DistroXImageV1Request;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;

@Prototype
public class DistroXImageTestDto extends AbstractCloudbreakTestDto<DistroXImageV1Request, Response, DistroXImageTestDto> {
    private static final String DEFAULT_SETTING_NAME = "test-image-setting" + '-' + UUID.randomUUID().toString().replaceAll("-", "");

    public DistroXImageTestDto(TestContext testContext) {
        super(new DistroXImageV1Request(), testContext);
    }

    @Override
    public DistroXImageTestDto valid() {
        return getCloudProvider().imageSettings(withName(getResourcePropertyProvider().getName()));
    }

    public DistroXImageTestDto withImageCatalog(String imageCatalog) {
        getRequest().setCatalog(imageCatalog);
        return this;
    }

    public DistroXImageTestDto withImageCatalog() {
        getRequest().setCatalog(getTestContext().get(ImageCatalogTestDto.class).getRequest().getName());
        return this;
    }

    public DistroXImageTestDto withImageId(String imageId) {
        getRequest().setId(imageId);
        return this;
    }

    public DistroXImageTestDto withName(String name) {
        setName(name);
        return this;
    }

    @Override
    public String getName() {
        return super.getName() == null ? DEFAULT_SETTING_NAME : super.getName();
    }
}

