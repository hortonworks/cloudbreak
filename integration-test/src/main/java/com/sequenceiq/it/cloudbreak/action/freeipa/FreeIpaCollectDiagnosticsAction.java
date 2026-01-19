package com.sequenceiq.it.cloudbreak.action.freeipa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaDiagnosticsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaCollectDiagnosticsAction implements Action<FreeIpaDiagnosticsTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCollectDiagnosticsAction.class);

    @Override
    public FreeIpaDiagnosticsTestDto action(TestContext testContext, FreeIpaDiagnosticsTestDto testDto, FreeIpaClient client) throws Exception {
        Log.whenJson(LOGGER, " FreeIPA collect diagnostics request: ", testDto.getRequest());
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext)
                .getDiagnosticsEndpoint()
                .collectDiagnostics(testDto.getRequest());
        testDto.setFlow("FreeIPA diagnostic collection", flowIdentifier);
        Log.log(LOGGER, " FreeIPA name: %s", client.getDefaultClient(testContext).getFreeIpaV1Endpoint()
                .describe(testDto.getRequest().getEnvironmentCrn()).getName());
        return testDto;
    }
}
