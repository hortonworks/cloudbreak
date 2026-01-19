package com.sequenceiq.it.cloudbreak.action.freeipa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTrustCommandsDto;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaTrustSetupCommandsAction extends AbstractFreeIpaAction<FreeIpaTrustCommandsDto> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaTrustSetupCommandsAction.class);

    @Override
    public FreeIpaTrustCommandsDto freeIpaAction(TestContext testContext, FreeIpaTrustCommandsDto testDto, FreeIpaClient client) throws Exception {
        testDto.setResponse(client.getDefaultClient(testContext).getTrustV1Endpoint().getTrustSetupCommands(testDto.getEnvironmentCrn()));
        return testDto;
    }
}
