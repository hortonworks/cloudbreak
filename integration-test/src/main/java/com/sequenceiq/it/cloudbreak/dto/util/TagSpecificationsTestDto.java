package com.sequenceiq.it.cloudbreak.dto.util;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.TagSpecificationsV4Response;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class TagSpecificationsTestDto extends AbstractCloudbreakTestDto<Object, TagSpecificationsV4Response, TagSpecificationsTestDto> {

    protected TagSpecificationsTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public TagSpecificationsTestDto valid() {
        return this;
    }

    @Override
    public int order() {
        return 500;
    }

}
