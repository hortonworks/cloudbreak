package com.sequenceiq.it.cloudbreak.newway.action.version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.version.VersionCheckTestDto;

public class VersionCheckTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionCheckTestAction.class);

    private VersionCheckTestAction() {
    }

    public static VersionCheckTestDto getCheckClientVersion(TestContext testContext, VersionCheckTestDto entity, CloudbreakClient client) {
        String logInitMessage = "Obtaining client version";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(client.getCloudbreakClient().utilV4Endpoint().checkClientVersion(entity.getVersion()));
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}
