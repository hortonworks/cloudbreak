package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class EnvironmentStopAction implements Action<EnvironmentTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentStopAction.class);

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        environmentClient.getEnvironmentClient()
                .environmentV1Endpoint()
                .postStopByCrn(testDto.getResponse().getCrn());
        Log.when(LOGGER, "Environment stop action posted");

        return testDto;
    }
}
