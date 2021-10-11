package com.sequenceiq.it.cloudbreak.dto.freeipa;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.imagecatalog.ChangeImageCatalogRequest;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;

@Prototype
public class FreeipaChangeImageCatalogTestDto extends AbstractFreeIpaTestDto<ChangeImageCatalogRequest, Void, FreeipaChangeImageCatalogTestDto> {

    protected FreeipaChangeImageCatalogTestDto(TestContext testContext) {
        super(new ChangeImageCatalogRequest(), testContext);
    }

    @Override
    public FreeipaChangeImageCatalogTestDto valid() {
        final String currentCatalog = getTestContext().given(FreeIpaTestDto.class).getResponse().getImage().getCatalog();
        getRequest().setImageCatalog(currentCatalog);
        return this;
    }

    public FreeipaChangeImageCatalogTestDto withImageCatalog(String imageCatalog) {
        getRequest().setImageCatalog(imageCatalog);
        return this;
    }
}
