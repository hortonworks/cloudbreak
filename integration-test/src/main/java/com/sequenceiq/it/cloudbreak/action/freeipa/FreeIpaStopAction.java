package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.stop.StopFreeIpaV1Response;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaStopAction extends AbstractFreeIpaAction<FreeIpaTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaStopAction.class);

    @Override
    protected FreeIpaTestDto freeIpaAction(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        String environmentCrn = testContext.given(EnvironmentTestDto.class).getCrn();
        Log.when(LOGGER, format(" FreeIPA CRN: %s", environmentCrn));
        Log.whenJson(LOGGER, format(" FreeIPA stop request: %n"), testDto.getRequest());
        StopFreeIpaV1Response stop = client.getDefaultClient(testContext)
                .getFreeIpaV1Endpoint()
                .stop(environmentCrn);
        testDto.setFlow("FreeIpaStopFlow", stop.getFlowIdentifier());
        return testDto;
    }
}
