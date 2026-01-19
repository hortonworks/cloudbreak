package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentStopAction extends AbstractEnvironmentAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentStopAction.class);

    @Override
    protected EnvironmentTestDto environmentAction(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) {
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext)
                .environmentV1Endpoint()
                .postStopByCrn(testDto.getResponse().getCrn());
        testDto.setLastKnownFlow(flowIdentifier);
        Log.when(LOGGER, "Environment stop action posted");
        return testDto;
    }
}
