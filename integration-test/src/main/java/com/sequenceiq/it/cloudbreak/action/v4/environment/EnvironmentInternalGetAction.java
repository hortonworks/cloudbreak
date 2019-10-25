package com.sequenceiq.it.cloudbreak.action.v4.environment;

import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class EnvironmentInternalGetAction implements Action<EnvironmentTestDto, EnvironmentClient> {

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto,
            EnvironmentClient environmentClient) throws Exception {
        if (testDto.getResponse() == null) {
            throw new IllegalArgumentException("Environment get action with internal actor requires a Environment response first.");
        }
        testDto.setResponse(
                environmentClient.getEnvironmentInternalCrnClient()
                        .withInternalCrn()
                        .environmentV1Endpoint()
                        .getByCrn(testDto.getResponse().getCrn()));
        Log.logJSON("Environment get response with internal actor: ", testDto.getResponse());
        return testDto;
    }
}
