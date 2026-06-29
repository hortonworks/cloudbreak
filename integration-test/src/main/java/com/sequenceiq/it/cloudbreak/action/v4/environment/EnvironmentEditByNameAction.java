package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentEditByNameAction extends AbstractEnvironmentAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentEditByNameAction.class);

    private final EnvironmentEditRequest environmentEditRequest;

    public EnvironmentEditByNameAction(EnvironmentEditRequest environmentEditRequest) {
        this.environmentEditRequest = environmentEditRequest;
    }

    @Override
    protected EnvironmentTestDto environmentAction(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) throws Exception {
        DetailedEnvironmentResponse response = client.getDefaultClient(testContext)
                .environmentV1Endpoint()
                .editByName(testDto.getResponse().getName(), environmentEditRequest);
        testDto.setResponse(response);
        Log.when(LOGGER, "Environment edit action posted");
        return testDto;
    }
}
