package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentEncryptionProfileDisableAction implements Action<EnvironmentTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentEncryptionProfileDisableAction.class);

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        Log.when(LOGGER, String.format("Disabling encryption profile, env name: %s", testDto.getName()));
        FlowIdentifier flowIdentifier = environmentClient
                .getDefaultClient(testContext)
                .environmentV1Endpoint()
                .disableEncryptionProfileByName(testDto.getName());
        testDto.setFlow("Disable Encryption Profile", flowIdentifier);
        Log.when(LOGGER, "Disable Encryption Profile was triggered successfully");
        return testDto;
    }
}
