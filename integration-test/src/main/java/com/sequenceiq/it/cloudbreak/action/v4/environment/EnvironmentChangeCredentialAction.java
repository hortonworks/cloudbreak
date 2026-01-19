package com.sequenceiq.it.cloudbreak.action.v4.environment;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentChangeCredentialAction implements Action<EnvironmentTestDto, EnvironmentClient> {

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        testDto.setResponse(
                environmentClient.getDefaultClient(testContext)
                        .environmentV1Endpoint()
                        .changeCredentialByEnvironmentName(testDto.getName(), testDto.getEnviornmentChangeCredentialRequest()));

        Log.whenJson("Environment change credential request: ", testDto.getEnviornmentChangeCredentialRequest());
        return testDto;
    }
}