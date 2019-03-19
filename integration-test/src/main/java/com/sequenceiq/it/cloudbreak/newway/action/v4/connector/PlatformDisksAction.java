package com.sequenceiq.it.cloudbreak.newway.action.v4.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.connector.PlatformDiskTestDto;

public class PlatformDisksAction implements Action<PlatformDiskTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformDisksAction.class);

    @Override
    public PlatformDiskTestDto action(TestContext testContext, PlatformDiskTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Obtaining disk types";
        LOGGER.info("{}", logInitMessage);
        testDto.setResponse(cloudbreakClient.getCloudbreakClient().connectorV4Endpoint().getDisktypes(cloudbreakClient.getWorkspaceId()));
        LOGGER.info("{} was successful", logInitMessage);
        return testDto;
    }
}
