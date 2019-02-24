package com.sequenceiq.it.cloudbreak.newway.action.stackmatrix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.stackmatrix.StackMatrixTestDto;

public class StackMatrixTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackMatrixTestAction.class);

    private StackMatrixTestAction() {
    }

    public static StackMatrixTestDto getStackMatrix(TestContext testContext, StackMatrixTestDto entity, CloudbreakClient client) {
        String logInitMessage = "Obtaining stack matrix";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(client.getCloudbreakClient().utilV4Endpoint().getStackMatrix());
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}
