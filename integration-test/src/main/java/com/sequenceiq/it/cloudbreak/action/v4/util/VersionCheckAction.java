package com.sequenceiq.it.cloudbreak.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.VersionCheckTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class VersionCheckAction implements Action<VersionCheckTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionCheckAction.class);

    @Override
    public VersionCheckTestDto action(TestContext testContext, VersionCheckTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        testDto.setResponse(cloudbreakClient.getDefaultClient(testContext).utilV4Endpoint().checkClientVersion(testDto.getVersion()));
        Log.whenJson(LOGGER, "Obtaining client version response:\n", testDto.getResponse());

        return testDto;    }
}
