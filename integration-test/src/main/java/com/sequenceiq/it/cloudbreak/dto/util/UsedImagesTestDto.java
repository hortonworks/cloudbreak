package com.sequenceiq.it.cloudbreak.dto.util;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.UsedImagesListV4Response;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class UsedImagesTestDto extends AbstractCloudbreakTestDto<Object, UsedImagesListV4Response, UsedImagesTestDto> {

    protected UsedImagesTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public UsedImagesTestDto valid() {
        return this;
    }

    @Override
    public int order() {
        return 500;
    }

}
