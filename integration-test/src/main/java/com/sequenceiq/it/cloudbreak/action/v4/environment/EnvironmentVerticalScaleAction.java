package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.verticalscale.VerticalScalingTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class EnvironmentVerticalScaleAction extends AbstractEnvironmentAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentVerticalScaleAction.class);

    private final String verticalScaleKey;

    public EnvironmentVerticalScaleAction(String verticalScaleKey) {
        this.verticalScaleKey = verticalScaleKey;
    }

    @Override
    protected EnvironmentTestDto environmentAction(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) {
        if (testContext.getCloudProvider().verticalScalingSupported()) {
            VerticalScalingTestDto verticalScalingTestDto = testContext.get(verticalScaleKey);
            FlowIdentifier flowIdentifier = client.getDefaultClient()
                    .environmentV1Endpoint()
                    .verticalScalingByCrn(testDto.getResponse().getCrn(), verticalScalingTestDto.getRequest());
            testDto.setLastKnownFlow(flowIdentifier);
        }
        Log.when(LOGGER, "Environment vertical scale action put");
        return testDto;
    }
}
