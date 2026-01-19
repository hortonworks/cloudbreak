package com.sequenceiq.it.cloudbreak.action.sdx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxDiagnosticsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class SdxCollectDiagnosticsAction implements Action<SdxDiagnosticsTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCollectDiagnosticsAction.class);

    @Override
    public SdxDiagnosticsTestDto action(TestContext testContext, SdxDiagnosticsTestDto testDto, SdxClient client) throws Exception {
        Log.whenJson(LOGGER, " SDX collect diagnostics request: ", testDto.getRequest());
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext)
                .diagnosticsEndpoint()
                .collectDiagnostics(testDto.getRequest());
        testDto.setFlow("SDX diagnostic collection", flowIdentifier);
        Log.log(LOGGER, " SDX name: %s", client.getDefaultClient(testContext).sdxEndpoint().get(testDto.getName()).getName());
        return testDto;
    }
}
