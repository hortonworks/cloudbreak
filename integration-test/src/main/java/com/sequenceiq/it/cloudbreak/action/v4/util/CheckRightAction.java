package com.sequenceiq.it.cloudbreak.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.authorization.info.model.CheckRightV4Request;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.CheckRightTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class CheckRightAction implements Action<CheckRightTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckRightAction.class);

    @Override
    public CheckRightTestDto action(TestContext testContext, CheckRightTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        CheckRightV4Request checkRightV4Request = new CheckRightV4Request();
        checkRightV4Request.setRights(testDto.getRightsToCheck());
        testDto.setResponse(cloudbreakClient.getDefaultClient(testContext).authorizationUtilEndpoint().checkRightInAccount(checkRightV4Request));
        Log.whenJson(LOGGER, "check rights in account response:\n", testDto.getResponse());
        return testDto;
    }
}
