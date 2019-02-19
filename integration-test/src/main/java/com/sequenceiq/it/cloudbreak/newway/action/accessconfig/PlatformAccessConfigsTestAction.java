package com.sequenceiq.it.cloudbreak.newway.action.accessconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.accessconfig.PlatformAccessConfigsTestDto;

public class PlatformAccessConfigsTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformAccessConfigsTestAction.class);

    private PlatformAccessConfigsTestAction() {
    }

    public static PlatformAccessConfigsTestDto getAccessConfigs(TestContext testContext, PlatformAccessConfigsTestDto entity, CloudbreakClient client) {
        String logInitMessage = "Obtaining access configs by credential";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(
                client.getCloudbreakClient().connectorV4Endpoint().getAccessConfigs(
                        client.getWorkspaceId(), entity.getCredentialName(), entity.getRegion(), entity.getPlatformVariant(), entity.getAvailabilityZone())
        );
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}
