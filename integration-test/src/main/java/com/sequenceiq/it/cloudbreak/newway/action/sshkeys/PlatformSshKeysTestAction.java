package com.sequenceiq.it.cloudbreak.newway.action.sshkeys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.sshkeys.PlatformSshKeysTestDto;

public class PlatformSshKeysTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformSshKeysTestAction.class);

    private PlatformSshKeysTestAction() {
    }

    public static PlatformSshKeysTestDto getSSHKeys(TestContext testContext, PlatformSshKeysTestDto entity, CloudbreakClient client) {
        String logInitMessage = "Obtaining ssh keys by credential";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(
                client.getCloudbreakClient().connectorV4Endpoint().getCloudSshKeys(
                        client.getWorkspaceId(), entity.getCredentialName(), entity.getRegion(), entity.getPlatformVariant(), entity.getAvailabilityZone())
        );
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}
