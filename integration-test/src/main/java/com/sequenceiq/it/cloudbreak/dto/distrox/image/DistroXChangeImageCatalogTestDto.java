package com.sequenceiq.it.cloudbreak.dto.distrox.image;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ChangeImageCatalogV4Request;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;

@Prototype
public class DistroXChangeImageCatalogTestDto extends AbstractCloudbreakTestDto<ChangeImageCatalogV4Request, Void, DistroXChangeImageCatalogTestDto> {

    protected DistroXChangeImageCatalogTestDto(TestContext testContext) {
        super(new ChangeImageCatalogV4Request(), testContext);
    }

    @Override
    public DistroXChangeImageCatalogTestDto valid() {
        final String currentCatalog = getTestContext().given(DistroXTestDto.class).getResponse().getImage().getCatalogName();
        getRequest().setImageCatalog(currentCatalog);
        return this;
    }

    public DistroXChangeImageCatalogTestDto withImageCatalog(String imageCatalog) {
        getRequest().setImageCatalog(imageCatalog);
        return this;
    }
}
