package com.sequenceiq.it.cloudbreak.newway.action.encryptionkeys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.encryption.PlatformEncryptionKeysTestDto;

public class PlatformEncryptionKeysTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformEncryptionKeysTestAction.class);

    private PlatformEncryptionKeysTestAction() {
    }

    public static PlatformEncryptionKeysTestDto getEncryptionKeys(TestContext testContext, PlatformEncryptionKeysTestDto entity, CloudbreakClient client) {
        String logInitMessage = "Obtaining encryption keys by credential";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(
                client.getCloudbreakClient().connectorV4Endpoint().getEncryptionKeys(
                        client.getWorkspaceId(), entity.getCredentialName(), entity.getRegion(), entity.getPlatformVariant(), entity.getAvailabilityZone())
        );
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}
