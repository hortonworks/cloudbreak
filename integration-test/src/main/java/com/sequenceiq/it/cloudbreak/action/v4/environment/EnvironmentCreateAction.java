package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentCreateAction extends AbstractEnvironmentAction {

    @Override
    protected EnvironmentTestDto environmentAction(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) throws Exception {
        if (!StringUtils.equals(testContext.getExistingResourceNames().get(EnvironmentTestDto.class), testDto.getName())) {
            Log.whenJson("Environment post request: ", testDto.getRequest());
            testDto.setResponse(client.getDefaultClient().environmentV1Endpoint().post(testDto.getRequest()));
        } else {
            testDto.setResponse(client.getDefaultClient().environmentV1Endpoint().getByName(testDto.getName()));
        }
        Log.whenJson("Environment response: ", testDto.getResponse());
        return testDto;
    }
}