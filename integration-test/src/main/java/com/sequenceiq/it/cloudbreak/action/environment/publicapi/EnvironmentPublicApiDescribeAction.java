package com.sequenceiq.it.cloudbreak.action.environment.publicapi;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.envpublicapi.EnvironmentPublicApiTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentPublicApiClient;

public class EnvironmentPublicApiDescribeAction implements Action<EnvironmentPublicApiTestDto, EnvironmentPublicApiClient> {
    @Override
    public EnvironmentPublicApiTestDto action(TestContext testContext, EnvironmentPublicApiTestDto testDto, EnvironmentPublicApiClient client) throws Exception {
        testDto.setResponse(
                client.getDefaultClient(testContext).describeEnvironment(testDto.getRequest())
        );
        Log.whenJson("Environment describe response: ", testDto.getResponse());
        return testDto;
    }
}
