package com.sequenceiq.it.cloudbreak.newway.action.v4.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.connector.PlatformIpPoolsTestDto;

public class PlatformIpPoolsAction implements Action<PlatformIpPoolsTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformIpPoolsAction.class);

    @Override
    public PlatformIpPoolsTestDto action(TestContext testContext, PlatformIpPoolsTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Obtaining ip pools by credential";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().connectorV4Endpoint().getIpPoolsCredentialId(
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
