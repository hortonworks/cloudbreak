package com.sequenceiq.it.cloudbreak.action.v4.environment;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.EnvironmentServiceClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class EnvironmentDeleteAction implements Action<EnvironmentTestDto, EnvironmentServiceClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDeleteAction.class);

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentServiceClient client) throws Exception {
        Log.log(LOGGER, format(" Name: %s", testDto.getRequest().getName()));
        Log.logJSON(LOGGER, format(" Environment request:%n"), testDto.getRequest());
        SimpleEnvironmentResponse delete = client.getEnvironmentServiceClient()
                .environmentV1Endpoint()
                .deleteByCrn(testDto.getResponseSimpleEnv().getCrn());
        testDto.setResponseSimpleEnv(delete);
        Log.logJSON(LOGGER, format(" Environment deleted successfully: %n"), delete);
        Log.log(LOGGER, format(" Environment CRN: %s", delete.getCrn()));
        return testDto;
    }
}