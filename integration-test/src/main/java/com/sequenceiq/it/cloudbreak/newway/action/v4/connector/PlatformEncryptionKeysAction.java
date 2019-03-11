package com.sequenceiq.it.cloudbreak.newway.action.v4.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.connector.PlatformEncryptionKeysTestDto;

public class PlatformEncryptionKeysAction implements Action<PlatformEncryptionKeysTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformEncryptionKeysAction.class);

    @Override
    public PlatformEncryptionKeysTestDto action(TestContext testContext,
                                                PlatformEncryptionKeysTestDto entity,
                                                CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Obtaining encryption keys by credential";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().connectorV4Endpoint().getEncryptionKeys(
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
