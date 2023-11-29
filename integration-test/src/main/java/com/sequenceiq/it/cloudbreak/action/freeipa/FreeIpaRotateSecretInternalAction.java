package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaRotationTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaRotateSecretInternalAction implements Action<FreeIpaRotationTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRotateSecretInternalAction.class);

    public FreeIpaRotateSecretInternalAction() {
    }

    @Override
    public FreeIpaRotationTestDto action(TestContext testContext, FreeIpaRotationTestDto testDto, FreeIpaClient client) throws Exception {
        Log.whenJson(LOGGER, format(" FreeIPA secret rotation request:%n"), testDto.getRequest());
        testDto.setFlow("FreeIPA secret rotation", client.getInternalClient(testContext)
                .getFreeipaRotationV1Endpoint()
                .rotateSecretsByCrn(testDto.getEnvironmentCrn(), testDto.getRequest()));
        Log.whenJson(LOGGER, format(" FreeIPA secret rotation started: %n"), testDto.getResponse());
        return testDto;
    }
}
