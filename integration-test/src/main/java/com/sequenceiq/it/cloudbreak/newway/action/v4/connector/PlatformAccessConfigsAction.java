package com.sequenceiq.it.cloudbreak.newway.action.v4.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.connector.PlatformAccessConfigsTestDto;

public class PlatformAccessConfigsAction implements Action<PlatformAccessConfigsTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformAccessConfigsAction.class);

    @Override
    public PlatformAccessConfigsTestDto action(TestContext testContext,
        PlatformAccessConfigsTestDto entity,
        CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Obtaining access configs by credential";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().connectorV4Endpoint().getAccessConfigs(
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
