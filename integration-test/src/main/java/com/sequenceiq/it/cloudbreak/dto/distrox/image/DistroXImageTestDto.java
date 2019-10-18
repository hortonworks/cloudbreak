package com.sequenceiq.it.cloudbreak.dto.distrox.image;

import javax.ws.rs.core.Response;

import com.sequenceiq.distrox.api.v1.distrox.model.image.DistroXImageV1Request;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;

@Prototype
public class DistroXImageTestDto extends AbstractCloudbreakTestDto<DistroXImageV1Request, Response, DistroXImageTestDto> {

    public DistroXImageTestDto(TestContext testContext) {
        super(new DistroXImageV1Request(), testContext);
    }

    @Override
    public DistroXImageTestDto valid() {
        return getCloudProvider().imageSettings(this);
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
}

