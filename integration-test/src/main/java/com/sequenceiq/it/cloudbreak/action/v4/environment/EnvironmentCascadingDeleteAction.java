package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentCascadingDeleteAction implements Action<EnvironmentTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCascadingDeleteAction.class);

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        Log.when(LOGGER, "Environment cascading delete request, crn: " +  testDto.getResponse().getCrn());
        SimpleEnvironmentResponse delete = environmentClient.getDefaultClient(testContext)
                .environmentV1Endpoint()
                .deleteByCrn(testDto.getResponse().getCrn(), true, false);
        testDto.setResponseSimpleEnv(delete);
        Log.whenJson(LOGGER, " Environment cascading delete response: ", delete);
        return testDto;
    }
}
