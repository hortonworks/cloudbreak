package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rebuild.RebuildRequest;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaRebuildAction extends AbstractFreeIpaAction<FreeIpaTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRebuildAction.class);

    public FreeIpaRebuildAction() {
    }

    public FreeIpaTestDto freeIpaAction(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        String environmentCrn = testContext.given(EnvironmentTestDto.class).getCrn();
        Log.when(LOGGER, format(" FreeIPA CRN: %s", environmentCrn));
        RebuildRequest request = new RebuildRequest();
        request.setEnvironmentCrn(environmentCrn);
        request.setSourceCrn(testDto.getCrn());
        Log.whenJson(LOGGER, format(" FreeIPA rebuild request: %n"), request);
        testDto.setResponse(
                client.getDefaultClient()
                        .getFreeIpaV1Endpoint()
                        .rebuild(request));
        Log.whenJson(LOGGER, format(" FreeIPA rebuilt successfully:%n"), testDto.getResponse());
        Log.when(LOGGER, format(" FreeIPA CRN: %s", testDto.getResponse().getCrn()));
        return testDto;
    }
}
