package com.sequenceiq.it.cloudbreak.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.StackMatrixTestDto;

public class StackMatrixAction implements Action<StackMatrixTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackMatrixAction.class);

    @Override
    public StackMatrixTestDto action(TestContext testContext, StackMatrixTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Obtaining stack matrix";
        LOGGER.info("{}", logInitMessage);
        testDto.setResponse(cloudbreakClient.getCloudbreakClient().utilV4Endpoint().getStackMatrix());
        LOGGER.info("{} was successful", logInitMessage);
        return testDto;
    }
}
