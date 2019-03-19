package com.sequenceiq.it.cloudbreak.newway.action.v4.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.connector.PlatformGatewaysTestDto;

public class PlatformGatewaysAction implements Action<PlatformGatewaysTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformGatewaysAction.class);

    @Override
    public PlatformGatewaysTestDto action(TestContext testContext, PlatformGatewaysTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Obtaining gateways by credential";
        LOGGER.info("{}", logInitMessage);
        testDto.setResponse(
                cloudbreakClient.getCloudbreakClient().connectorV4Endpoint().getGatewaysCredentialId(
                        cloudbreakClient.getWorkspaceId(),
                        testDto.getCredentialName(),
                        testDto.getRegion(),
                        testDto.getPlatformVariant(),
                        testDto.getAvailabilityZone())
        );
        LOGGER.info("{} was successful", logInitMessage);
        return testDto;
    }
}
