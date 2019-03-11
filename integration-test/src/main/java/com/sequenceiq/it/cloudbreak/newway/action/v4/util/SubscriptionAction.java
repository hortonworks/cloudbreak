package com.sequenceiq.it.cloudbreak.newway.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.util.SubscriptionTestDto;

public class SubscriptionAction implements Action<SubscriptionTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionAction.class);

    @Override
    public SubscriptionTestDto action(TestContext testContext, SubscriptionTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Subscribing";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(cloudbreakClient.getCloudbreakClient().utilV4Endpoint().subscribe(entity.getRequest()));
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }
}
