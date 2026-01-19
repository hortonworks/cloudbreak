package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaDeleteAction extends AbstractFreeIpaAction<FreeIpaTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDeleteAction.class);

    public FreeIpaTestDto freeIpaAction(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        String environmentCrn = testContext.given(EnvironmentTestDto.class).getCrn();
        Log.when(LOGGER, String.format(" FreeIPA crn: %s", environmentCrn));
        Log.whenJson(LOGGER, format(" FreeIPA delete:%n"), testDto.getRequest());
        client.getDefaultClient(testContext)
                .getFreeIpaV1Endpoint()
                .delete(environmentCrn, false);
        testDto.setResponse(
                client.getDefaultClient(testContext)
                        .getFreeIpaV1Endpoint()
                        .describe(environmentCrn)
        );
        Log.when(LOGGER, String.format(" FreeIPA deleted successfully. FreeIPA CRN: %s", testDto.getResponse().getCrn()));
        return testDto;
    }
}
