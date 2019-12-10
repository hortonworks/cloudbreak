package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class EnvironmentDeleteForcedAction implements Action<EnvironmentTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDeleteForcedAction.class);

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        Log.log(LOGGER, " Environment name: %s", testDto.getRequest().getName());
        Log.log(LOGGER, " Environment endpoint: %s", environmentClient.getEnvironmentClient().environmentV1Endpoint().toString());
        Log.logJSON(LOGGER, " Environment forced delete request: ", testDto.getRequest());
        SimpleEnvironmentResponse delete = environmentClient.getEnvironmentClient()
                .environmentV1Endpoint()
                .deleteByCrn(testDto.getResponse().getCrn(), true);
        Log.logJSON(LOGGER, " Environment forced delete response: ", delete);
        return testDto;
    }
}
