package com.sequenceiq.it.cloudbreak.action.freeipa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTrustCommandsDto;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaTrustCleanupCommandsAction extends AbstractFreeIpaAction<FreeIpaTrustCommandsDto> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaTrustCleanupCommandsAction.class);

    @Override
    public FreeIpaTrustCommandsDto freeIpaAction(TestContext testContext, FreeIpaTrustCommandsDto testDto, FreeIpaClient client) throws Exception {
        testDto.setResponse(client.getDefaultClient().getTrustV1Endpoint().getTrustCleanupCommands(testDto.getEnvironmentCrn()));
        return testDto;
    }
}
