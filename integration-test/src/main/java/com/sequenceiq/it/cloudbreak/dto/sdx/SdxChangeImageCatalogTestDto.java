package com.sequenceiq.it.cloudbreak.dto.sdx;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.sdx.api.model.SdxChangeImageCatalogRequest;

@Prototype
public class SdxChangeImageCatalogTestDto extends AbstractSdxTestDto<SdxChangeImageCatalogRequest, Void, SdxChangeImageCatalogTestDto> {

    protected SdxChangeImageCatalogTestDto(TestContext testContext) {
        super(new SdxChangeImageCatalogRequest(), testContext);
    }

    @Override
    public SdxChangeImageCatalogTestDto valid() {
        final String currentCatalog = getTestContext().given(SdxInternalTestDto.class).getResponse().getStackV4Response().getImage().getCatalogName();
        getRequest().setImageCatalog(currentCatalog);
        return this;
    }

    public SdxChangeImageCatalogTestDto withImageCatalog(String imageCatalog) {
        getRequest().setImageCatalog(imageCatalog);
        return this;
    }
}
