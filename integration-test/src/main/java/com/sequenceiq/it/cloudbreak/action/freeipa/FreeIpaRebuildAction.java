package com.sequenceiq.it.cloudbreak.action.freeipa;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rebuild.RebuildRequest;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class FreeIpaRebuildAction implements Action<FreeIpaTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRebuildAction.class);

    public FreeIpaRebuildAction() {
    }

    public FreeIpaTestDto action(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        Log.when(LOGGER, format(" FreeIPA CRN: %s", testDto.getRequest().getEnvironmentCrn()));
        RebuildRequest request = new RebuildRequest();
        request.setEnvironmentCrn(testDto.getRequest().getEnvironmentCrn());
        request.setSourceCrn(testDto.getCrn());
        Log.whenJson(LOGGER, format(" FreeIPA rebuild request: %n"), request);
        client.getDefaultClient()
                .getFreeIpaV1Endpoint()
                .rebuild(request);
        return testDto;
    }
}
