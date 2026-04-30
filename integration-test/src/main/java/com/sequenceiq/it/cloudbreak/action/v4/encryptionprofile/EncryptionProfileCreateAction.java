package com.sequenceiq.it.cloudbreak.action.v4.encryptionprofile;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EncryptionProfileTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EncryptionProfileCreateAction implements Action<EncryptionProfileTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionProfileCreateAction.class);

    @Override
    public EncryptionProfileTestDto action(TestContext testContext, EncryptionProfileTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        Log.when(LOGGER, "Encryption Profile create request: " + testDto.getRequest());
        testDto.setResponse(
                environmentClient.getDefaultSilentClient(testContext)
                        .encryptionProfileEndpoint()
                        .create(testDto.getRequest()));
        Log.whenJson(LOGGER, "Encryption Profile was created successfully:\n", testDto.getResponse());
        Log.when(LOGGER, format(" CRN: %s", testDto.getResponse().getCrn()));

        return testDto;
    }
}
