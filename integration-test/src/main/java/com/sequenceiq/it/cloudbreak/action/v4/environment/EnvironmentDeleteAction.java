package com.sequenceiq.it.cloudbreak.action.v4.environment;

import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentDeleteAction implements Action<EnvironmentTestDto, EnvironmentClient> {

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        SimpleEnvironmentResponse delete = environmentClient.getDefaultClient(testContext)
                .environmentV1Endpoint()
                .deleteByCrn(testDto.getResponse().getCrn(), true, false);
        testDto.setResponseSimpleEnv(delete);
        Log.whenJson("Environment delete response: ", delete);
        return testDto;
    }
}
