package com.sequenceiq.it.cloudbreak.newway.action.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.gateway.PlatformGatewaysTestDto;

public class PlatformGatewaysTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformGatewaysTestAction.class);

    private PlatformGatewaysTestAction() {
    }

    public static PlatformGatewaysTestDto getGateways(TestContext testContext, PlatformGatewaysTestDto entity, CloudbreakClient client) {
        String logInitMessage = "Obtaining gateways by credential";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(
                client.getCloudbreakClient().connectorV4Endpoint().getGatewaysCredentialId(
                        client.getWorkspaceId(), entity.getCredentialName(), entity.getRegion(), entity.getPlatformVariant(), entity.getAvailabilityZone())
        );
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}
