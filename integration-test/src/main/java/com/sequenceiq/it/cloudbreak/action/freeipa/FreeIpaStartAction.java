package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.start.StartFreeIpaV1Response;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaStartAction extends AbstractFreeIpaAction<FreeIpaTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaStartAction.class);

    @Override
    protected FreeIpaTestDto freeIpaAction(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        String environmentCrn = testContext.given(EnvironmentTestDto.class).getCrn();
        Log.when(LOGGER, format(" FreeIPA CRN: %s", environmentCrn));
        Log.whenJson(LOGGER, format(" FreeIPA start request: %n"), testDto.getRequest());
        StartFreeIpaV1Response start = client.getDefaultClient()
                .getFreeIpaV1Endpoint()
                .start(environmentCrn);
        testDto.setFlow("FreeIpaStartFlow", start.getFlowIdentifier());
        return testDto;
    }
}
