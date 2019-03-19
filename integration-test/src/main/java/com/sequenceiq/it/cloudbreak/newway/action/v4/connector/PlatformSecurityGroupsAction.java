package com.sequenceiq.it.cloudbreak.newway.action.v4.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.connector.PlatformSecurityGroupsTestDto;

public class PlatformSecurityGroupsAction implements Action<PlatformSecurityGroupsTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformSecurityGroupsAction.class);

    @Override
    public PlatformSecurityGroupsTestDto action(TestContext testContext,
                                                PlatformSecurityGroupsTestDto testDto,
                                                CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Obtaining security groups by credential";
        LOGGER.info("{}", logInitMessage);
        testDto.setResponse(
                cloudbreakClient.getCloudbreakClient().connectorV4Endpoint().getSecurityGroups(
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
