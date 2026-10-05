package com.sequenceiq.it.cloudbreak.action.sdx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class SdxRestartRuntimeServicesAction<T extends AbstractSdxTestDto<?, ?, T>> implements Action<T, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRestartRuntimeServicesAction.class);

    private final boolean rollingRestart;

    private final boolean staleServicesOnly;

    public SdxRestartRuntimeServicesAction(boolean rollingRestart, boolean staleServicesOnly) {
        this.rollingRestart = rollingRestart;
        this.staleServicesOnly = staleServicesOnly;
    }

    @Override
    public T action(TestContext testContext, T testDto, SdxClient sdxClient) throws Exception {
        Log.when(LOGGER, String.format("Restarting runtime services, dl name: %s (rolling restart: %s, stale services only: %s)",
                testDto.getName(), rollingRestart, staleServicesOnly));
        FlowIdentifier flowIdentifier = sdxClient
                .getDefaultClient(testContext)
                .sdxEndpoint()
                .restartClusterServicesByCrn(testDto.getCrn(), rollingRestart, staleServicesOnly);
        testDto.setFlow("Restart runtime services", flowIdentifier);
        Log.when(LOGGER, "Restart runtime services started successfully");
        return testDto;
    }
}
