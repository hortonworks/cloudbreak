package com.sequenceiq.it.cloudbreak.newway.action.notificationtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.notificationtest.NotificationTestingTestDto;

public class NotificationTestingTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationTestingTestAction.class);

    private NotificationTestingTestAction() {
    }

    public static NotificationTestingTestDto postNotificationTest(TestContext testContext, NotificationTestingTestDto entity, CloudbreakClient client) {
        String logInitMessage = "Posting notification test";
        LOGGER.info("{}", logInitMessage);
        client.getCloudbreakClient().utilV4Endpoint().postNotificationTest();
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}
