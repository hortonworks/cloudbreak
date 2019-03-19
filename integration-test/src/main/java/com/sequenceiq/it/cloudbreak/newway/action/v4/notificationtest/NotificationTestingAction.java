package com.sequenceiq.it.cloudbreak.newway.action.v4.notificationtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.util.NotificationTestingTestDto;

public class NotificationTestingAction implements Action<NotificationTestingTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationTestingAction.class);

    @Override
    public NotificationTestingTestDto action(TestContext testContext, NotificationTestingTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Posting notification test";
        LOGGER.info("{}", logInitMessage);
        cloudbreakClient.getCloudbreakClient().utilV4Endpoint().postNotificationTest();
        LOGGER.info("{} was successful", logInitMessage);
        return testDto;
    }
}
