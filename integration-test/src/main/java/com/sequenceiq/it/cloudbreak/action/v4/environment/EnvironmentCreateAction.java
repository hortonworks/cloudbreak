package com.sequenceiq.it.cloudbreak.action.v4.environment;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.EnvironmentServiceClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class EnvironmentCreateAction implements Action<EnvironmentTestDto, EnvironmentServiceClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCreateAction.class);

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentServiceClient client) throws Exception {
        Log.log(LOGGER, String.format(" Name: %s", testDto.getRequest().getName()));
        Log.logJSON(LOGGER, format(" Environment create :%n"), testDto.getRequest());
        testDto.setResponse(client.getEnvironmentServiceClient()
                .environmentV1Endpoint()
                .post(testDto.getRequest()));
        Log.logJSON(LOGGER, format(" Environment created successfully. Crn: :%n"), testDto.getResponse().getCrn());
        return testDto;
    }
}