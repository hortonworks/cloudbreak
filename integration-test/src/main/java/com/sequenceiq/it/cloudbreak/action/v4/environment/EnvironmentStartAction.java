package com.sequenceiq.it.cloudbreak.action.v4.environment;

import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

public class EnvironmentStartAction implements Action<EnvironmentTestDto, EnvironmentClient> {

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        environmentClient.getEnvironmentClient()
                .environmentV1Endpoint()
                .postStartByCrn(testDto.getResponse().getCrn());
        return testDto;
    }
}
