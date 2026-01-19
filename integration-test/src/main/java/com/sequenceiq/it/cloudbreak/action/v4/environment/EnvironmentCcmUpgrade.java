package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentCcmUpgrade implements Action<EnvironmentTestDto, EnvironmentClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCcmUpgrade.class);

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) throws Exception {
        Log.when(LOGGER, "Environment upgrading ccm request, crn: " +  testDto.getResponse().getCrn());
        client.getDefaultClient(testContext)
                .environmentV1Endpoint().upgradeCcmByCrn(testDto.getResponse().getCrn());
        return testDto;
    }
}
