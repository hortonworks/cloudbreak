package com.sequenceiq.it.cloudbreak.action.v4.environment;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.EnvironmentServiceClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class EnvironmentListAction implements Action<EnvironmentTestDto, EnvironmentServiceClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentListAction.class);

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentServiceClient client) throws Exception {
        Log.log(LOGGER, "Environment list action");
        testDto.setResponseSimpleEnvSet(
                client.getEnvironmentServiceClient()
                        .environmentV1Endpoint()
                        .list());
        Log.logJSON(LOGGER, format(" Environment listed successfully: %n"), testDto.getResponseSimpleEnvSet());
        return testDto;
    }
}