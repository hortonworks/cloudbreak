package com.sequenceiq.it.cloudbreak.newway.action.subscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.subscription.SubscriptionTestDto;

public class SubscriptionTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionTestAction.class);

    private SubscriptionTestAction() {
    }

    public static SubscriptionTestDto getSubscribe(TestContext testContext, SubscriptionTestDto entity, CloudbreakClient client) {
        String logInitMessage = "Subscribing";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(client.getCloudbreakClient().utilV4Endpoint().subscribe(entity.getRequest()));
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}
