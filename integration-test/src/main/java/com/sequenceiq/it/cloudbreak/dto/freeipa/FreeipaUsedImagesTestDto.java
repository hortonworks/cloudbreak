package com.sequenceiq.it.cloudbreak.dto.freeipa;

import com.sequenceiq.freeipa.api.v1.util.model.UsedImagesListV1Response;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;

@Prototype
public class FreeipaUsedImagesTestDto extends AbstractFreeIpaTestDto<Object, UsedImagesListV1Response, FreeipaUsedImagesTestDto> {

    protected FreeipaUsedImagesTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public FreeipaUsedImagesTestDto valid() {
        return this;
    }

    @Override
    public int order() {
        return 500;
    }

}
