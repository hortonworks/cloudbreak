package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaRotationTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaRotateSecretAction extends AbstractFreeIpaAction<FreeIpaRotationTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRotateSecretAction.class);

    public FreeIpaRotateSecretAction() {
    }

    @Override
    public FreeIpaRotationTestDto freeIpaAction(TestContext testContext, FreeIpaRotationTestDto testDto, FreeIpaClient client) throws Exception {
        Log.whenJson(LOGGER, format(" FreeIPA secret rotation request:%n"), testDto.getRequest());
        String environmentCrn = testContext.given(EnvironmentTestDto.class).getCrn();
        testDto.setFlow("FreeIPA secret rotation", client.getDefaultClient()
                .getFreeipaRotationV1Endpoint()
                .rotateSecretsByCrn(environmentCrn, testDto.getRequest()));
        Log.whenJson(LOGGER, format(" FreeIPA secret rotation started: %n"), testDto.getResponse());
        return testDto;
    }
}
