package com.sequenceiq.it.cloudbreak.newway.action.v4.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.connector.PlatformAccessConfigsTestDto;

public class PlatformAccessConfigsAction implements Action<PlatformAccessConfigsTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformAccessConfigsAction.class);

    @Override
    public PlatformAccessConfigsTestDto action(TestContext testContext,
        PlatformAccessConfigsTestDto testDto,
        CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Obtaining access configs by credential";
        LOGGER.info("{}", logInitMessage);
        testDto.setResponse(
                cloudbreakClient.getCloudbreakClient().connectorV4Endpoint().getAccessConfigs(
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
