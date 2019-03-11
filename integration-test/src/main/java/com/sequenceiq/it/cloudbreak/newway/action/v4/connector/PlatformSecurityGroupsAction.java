package com.sequenceiq.it.cloudbreak.newway.action.v4.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.connector.PlatformSecurityGroupsTestDto;

public class PlatformSecurityGroupsAction implements Action<PlatformSecurityGroupsTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformSecurityGroupsAction.class);

    @Override
    public PlatformSecurityGroupsTestDto action(TestContext testContext,
                                                PlatformSecurityGroupsTestDto entity,
                                                CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Obtaining security groups by credential";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().connectorV4Endpoint().getSecurityGroups(
                        cloudbreakClient.getWorkspaceId(),
                        entity.getCredentialName(),
                        entity.getRegion(),
                        entity.getPlatformVariant(),
                        entity.getAvailabilityZone())
        );
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }
}
