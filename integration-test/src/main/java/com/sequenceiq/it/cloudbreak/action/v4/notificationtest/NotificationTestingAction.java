package com.sequenceiq.it.cloudbreak.action.v4.notificationtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.NotificationTestingTestDto;

public class NotificationTestingAction implements Action<NotificationTestingTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationTestingAction.class);

    @Override
    public NotificationTestingTestDto action(TestContext testContext, NotificationTestingTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Posting notification test";
        LOGGER.info("{}", logInitMessage);
        cloudbreakClient.getDefaultClient().utilV4Endpoint().postNotificationTest();
        LOGGER.info("{} was successful", logInitMessage);
        return testDto;
    }
}
