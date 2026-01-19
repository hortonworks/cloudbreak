package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.common.api.type.DataHubStartAction;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentStartAction extends AbstractEnvironmentAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentStartAction.class);

    @Override
    protected EnvironmentTestDto environmentAction(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) throws Exception {
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext)
                .environmentV1Endpoint()
                .postStartByCrn(testDto.getResponse().getCrn(), DataHubStartAction.START_ALL);
        testDto.setLastKnownFlow(flowIdentifier);
        Log.when(LOGGER, "Environment start action posted");
        return testDto;
    }
}
