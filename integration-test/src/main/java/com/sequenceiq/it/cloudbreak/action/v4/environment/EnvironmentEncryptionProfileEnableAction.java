package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentEncryptionProfileEnableAction implements Action<EnvironmentTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentEncryptionProfileEnableAction.class);

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        String encryptionProfileCrn = testDto.getRequest().getEncryptionProfileNameOrCrn();
        Log.when(LOGGER, String.format("Enabling encryption profile, env name: %s encryption profile CRN: %s", testDto.getName(), encryptionProfileCrn));
        FlowIdentifier flowIdentifier = environmentClient
                .getDefaultClient(testContext)
                .environmentV1Endpoint()
                .enableEncryptionProfileByName(testDto.getName(), encryptionProfileCrn);
        testDto.setFlow("Enable Encryption Profile", flowIdentifier);
        Log.when(LOGGER, "Encryption Profile was enabled successfully");
        return testDto;
    }
}
