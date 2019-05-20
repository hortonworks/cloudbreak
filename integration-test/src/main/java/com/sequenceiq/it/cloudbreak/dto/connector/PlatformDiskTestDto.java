package com.sequenceiq.it.cloudbreak.dto.connector;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformDisksV4Response;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;

@Prototype
public class PlatformDiskTestDto extends AbstractCloudbreakTestDto<Object, PlatformDisksV4Response, PlatformDiskTestDto> {

    protected PlatformDiskTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public PlatformDiskTestDto valid() {
        return this;
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.debug("this entry point does not have any clean up operation");
    }

    @Override
    public int order() {
        return 500;
    }

}
