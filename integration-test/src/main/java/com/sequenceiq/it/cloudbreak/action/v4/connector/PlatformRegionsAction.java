package com.sequenceiq.it.cloudbreak.action.v4.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.connector.PlatformRegionTestDto;

public class PlatformRegionsAction implements Action<PlatformRegionTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformRegionsAction.class);

    @Override
    public PlatformRegionTestDto action(TestContext testContext, PlatformRegionTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Obtaining regions by credential";
        LOGGER.info("{}", logInitMessage);
        testDto.setResponse(
                cloudbreakClient.getCloudbreakClient().connectorV4Endpoint().getRegionsByCredential(
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
