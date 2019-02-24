package com.sequenceiq.it.cloudbreak.newway.entity.notificationtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AbstractCloudbreakEntity;

@Prototype
public class NotificationTestingTestDto extends AbstractCloudbreakEntity<Object, Object, NotificationTestingTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationTestingTestDto.class);

    protected NotificationTestingTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public NotificationTestingTestDto valid() {
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
