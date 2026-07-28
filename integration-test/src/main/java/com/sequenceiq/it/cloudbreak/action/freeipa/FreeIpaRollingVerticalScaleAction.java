package com.sequenceiq.it.cloudbreak.action.freeipa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.common.api.type.OrchestratorType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.verticalscale.VerticalScalingTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class FreeIpaRollingVerticalScaleAction implements Action<EnvironmentTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRollingVerticalScaleAction.class);

    private final String verticalScaleKey;

    public FreeIpaRollingVerticalScaleAction(String verticalScaleKey) {
        this.verticalScaleKey = verticalScaleKey;
    }

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) throws Exception {
        VerticalScalingTestDto verticalScalingTestDto = testContext.get(verticalScaleKey);
        VerticalScaleRequest request = verticalScalingTestDto.getRequest();
        request.setOrchestratorType(OrchestratorType.ONE_BY_ONE.name());

        Log.whenJson(LOGGER, " FreeIPA rolling vertical scale request: ", request);

        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext)
                .environmentV1Endpoint()
                .verticalScalingByCrn(testDto.getResponse().getCrn(), request);

        testDto.setLastKnownFlow(flowIdentifier);
        Log.whenJson(LOGGER, " FreeIPA rolling vertical scale started: ", flowIdentifier);
        return testDto;
    }
}
