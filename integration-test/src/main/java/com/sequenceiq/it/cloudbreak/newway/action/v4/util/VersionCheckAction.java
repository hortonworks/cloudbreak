package com.sequenceiq.it.cloudbreak.newway.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.util.VersionCheckTestDto;

public class VersionCheckAction implements Action<VersionCheckTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionCheckAction.class);

    @Override
    public VersionCheckTestDto action(TestContext testContext, VersionCheckTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Obtaining client version";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(cloudbreakClient.getCloudbreakClient().utilV4Endpoint().checkClientVersion(entity.getVersion()));
        LOGGER.info("{} was successful", logInitMessage);
        return entity;    }
}
