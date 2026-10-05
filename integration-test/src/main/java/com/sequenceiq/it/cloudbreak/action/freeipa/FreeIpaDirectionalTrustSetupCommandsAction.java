package com.sequenceiq.it.cloudbreak.action.freeipa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaDirectionalTrustCommandsDto;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaDirectionalTrustSetupCommandsAction extends AbstractFreeIpaAction<FreeIpaDirectionalTrustCommandsDto> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDirectionalTrustSetupCommandsAction.class);

    @Override
    public FreeIpaDirectionalTrustCommandsDto freeIpaAction(TestContext testContext, FreeIpaDirectionalTrustCommandsDto testDto, FreeIpaClient client)
            throws Exception {
        testDto.setResponse(client.getDefaultClient(testContext).getTrustV1Endpoint().getDirectionalTrustSetupCommands(testDto.getEnvironmentCrn()));
        return testDto;
    }
}
