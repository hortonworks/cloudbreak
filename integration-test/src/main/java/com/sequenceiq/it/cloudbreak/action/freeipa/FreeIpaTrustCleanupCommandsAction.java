package com.sequenceiq.it.cloudbreak.action.freeipa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTrustCommandsDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaTrustCleanupCommandsAction extends AbstractFreeIpaAction<FreeIpaTrustCommandsDto> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaTrustCleanupCommandsAction.class);

    @Override
    public FreeIpaTrustCommandsDto freeIpaAction(TestContext testContext, FreeIpaTrustCommandsDto testDto, FreeIpaClient client) throws Exception {
        TrustSetupCommandsResponse response = client.getDefaultClient(testContext).getTrustV1Endpoint()
                .getTrustCleanupCommands(testDto.getEnvironmentCrn());
        if (response == null) {
            throw new TestFailException("Trust cleanup commands API returned null response for environment: " + testDto.getEnvironmentCrn());
        }
        testDto.setResponse(response);
        return testDto;
    }
}
