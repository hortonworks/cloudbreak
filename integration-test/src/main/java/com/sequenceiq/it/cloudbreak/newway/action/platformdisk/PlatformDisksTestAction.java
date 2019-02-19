package com.sequenceiq.it.cloudbreak.newway.action.platformdisk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.disk.PlatformDiskTestDto;

public class PlatformDisksTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformDisksTestAction.class);

    private PlatformDisksTestAction() {
    }

    public static PlatformDiskTestDto getDiskTypes(TestContext testContext, PlatformDiskTestDto entity, CloudbreakClient client) {
        String logInitMessage = "Obtaining disk types";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(client.getCloudbreakClient().connectorV4Endpoint().getDisktypes(client.getWorkspaceId()));
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}
