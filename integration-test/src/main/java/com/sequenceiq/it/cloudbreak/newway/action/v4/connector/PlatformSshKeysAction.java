package com.sequenceiq.it.cloudbreak.newway.action.v4.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.connector.PlatformSshKeysTestDto;

public class PlatformSshKeysAction implements Action<PlatformSshKeysTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformSshKeysAction.class);

    @Override
    public PlatformSshKeysTestDto action(TestContext testContext, PlatformSshKeysTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Obtaining ssh keys by credential";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().connectorV4Endpoint().getCloudSshKeys(
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
