package com.sequenceiq.it.cloudbreak.action.v4.environment;

import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class EnvironmentCreateAction extends AbstractEnvironmentAction {

    @Override
    protected EnvironmentTestDto environmentAction(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) throws Exception {
        Log.whenJson("Environment post request: ", testDto.getRequest());
        testDto.setResponse(
                client.getDefaultClient()
                        .environmentV1Endpoint()
                        .post(testDto.getRequest()));

        Log.whenJson("Environment post response: ", testDto.getResponse());
        return testDto;
    }
}