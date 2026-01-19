package com.sequenceiq.it.cloudbreak.action.sdx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCMDiagnosticsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class SdxCollectCMDiagnosticsAction implements Action<SdxCMDiagnosticsTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCollectCMDiagnosticsAction.class);

    @Override
    public SdxCMDiagnosticsTestDto action(TestContext testContext, SdxCMDiagnosticsTestDto testDto, SdxClient client) throws Exception {
        Log.whenJson(LOGGER, " SDX collect CM based diagnostics request: ", testDto.getRequest());
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext)
                .diagnosticsEndpoint()
                .collectCmDiagnostics(testDto.getRequest());
        testDto.setFlow("SDX CM based diagnostic collection", flowIdentifier);
        Log.log(LOGGER, " SDX name: %s", client.getDefaultClient(testContext).sdxEndpoint().get(testDto.getName()).getName());
        return testDto;
    }
}
