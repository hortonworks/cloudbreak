package com.sequenceiq.it.cloudbreak.newway.action.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.network.PlatformNetworksTestDto;

public class PlatformNetworksTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformNetworksTestAction.class);

    private PlatformNetworksTestAction() {
    }

    public static PlatformNetworksTestDto getPlatformNetworks(TestContext testContext, PlatformNetworksTestDto entity, CloudbreakClient client) {
        String logInitMessage = "Obtaining networks by credential";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(
                client.getCloudbreakClient().connectorV4Endpoint().getCloudNetworks(
                        client.getWorkspaceId(), entity.getCredentialName(), entity.getRegion(), entity.getPlatformVariant(), entity.getAvailabilityZone())
        );
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}
