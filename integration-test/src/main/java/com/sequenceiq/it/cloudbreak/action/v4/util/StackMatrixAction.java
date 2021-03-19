package com.sequenceiq.it.cloudbreak.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.StackMatrixTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class StackMatrixAction implements Action<StackMatrixTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackMatrixAction.class);

    @Override
    public StackMatrixTestDto action(TestContext testContext, StackMatrixTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        testDto.setResponse(cloudbreakClient.getDefaultClient().utilV4Endpoint().getStackMatrix(null, testDto.getCloudPlatform().name()));
        Log.whenJson(LOGGER, "Obtaining stack matrix response:\n", testDto.getResponse());

        return testDto;
    }
}
