package com.sequenceiq.it.cloudbreak.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.VersionCheckTestDto;

public class VersionCheckAction implements Action<VersionCheckTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionCheckAction.class);

    @Override
    public VersionCheckTestDto action(TestContext testContext, VersionCheckTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Obtaining client version";
        LOGGER.info("{}", logInitMessage);
        testDto.setResponse(cloudbreakClient.getCloudbreakClient().utilV4Endpoint().checkClientVersion(testDto.getVersion()));
        LOGGER.info("{} was successful", logInitMessage);
        return testDto;    }
}
