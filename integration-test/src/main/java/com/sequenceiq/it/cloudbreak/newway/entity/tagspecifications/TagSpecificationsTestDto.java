package com.sequenceiq.it.cloudbreak.newway.entity.tagspecifications;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.TagSpecificationsV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.CloudbreakEntity;

@Prototype
public class TagSpecificationsTestDto extends AbstractCloudbreakEntity<Object, TagSpecificationsV4Response, TagSpecificationsTestDto> {

    protected TagSpecificationsTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public CloudbreakEntity valid() {
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
