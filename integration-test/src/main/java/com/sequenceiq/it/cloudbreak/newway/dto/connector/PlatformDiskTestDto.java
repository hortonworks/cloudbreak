package com.sequenceiq.it.cloudbreak.newway.dto.connector;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformDisksV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.AbstractCloudbreakTestDto;

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
