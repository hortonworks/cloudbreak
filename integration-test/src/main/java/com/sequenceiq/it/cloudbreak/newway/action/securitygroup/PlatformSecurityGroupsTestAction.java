package com.sequenceiq.it.cloudbreak.newway.action.securitygroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.securitygroup.PlatformSecurityGroupsTestDto;

public class PlatformSecurityGroupsTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformSecurityGroupsTestAction.class);

    private PlatformSecurityGroupsTestAction() {
    }

    public static PlatformSecurityGroupsTestDto getSecurityGroups(TestContext testContext, PlatformSecurityGroupsTestDto entity, CloudbreakClient client) {
        String logInitMessage = "Obtaining security groups by credential";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(
                client.getCloudbreakClient().connectorV4Endpoint().getSecurityGroups(
                        client.getWorkspaceId(), entity.getCredentialName(), entity.getRegion(), entity.getPlatformVariant(), entity.getAvailabilityZone())
        );
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}
