package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeipaRotateSaltPasswordAction extends AbstractFreeIpaAction<FreeIpaTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDownscaleAction.class);

    @Override
    public FreeIpaTestDto freeIpaAction(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        String environmentCrn = testContext.given(EnvironmentTestDto.class).getCrn();
        Log.whenJson(LOGGER, format(" FreeIPA rotate salt password request for environment %n"), environmentCrn);
        FlowIdentifier flowIdentifier = client.getDefaultClient().getFreeIpaV1Endpoint().rotateSaltPassword(environmentCrn);
        testDto.setFlow("FreeIPA rotate salt password",  flowIdentifier);
        Log.whenJson(LOGGER, format(" FreeIPA rotate salt password started: %n"), environmentCrn);
        return testDto;
    }
}
