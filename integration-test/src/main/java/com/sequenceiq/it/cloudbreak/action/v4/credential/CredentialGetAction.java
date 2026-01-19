package com.sequenceiq.it.cloudbreak.action.v4.credential;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class CredentialGetAction implements Action<CredentialTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialGetAction.class);

    @Override
    public CredentialTestDto action(TestContext testContext, CredentialTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        Log.when(LOGGER, " Credential get request: " + testDto.getName());
        testDto.setResponse(
                environmentClient.getDefaultClient(testContext)
                        .credentialV1Endpoint()
                        .getByName(testDto.getName()));
        Log.whenJson(LOGGER, " Credential get successfully:\n", testDto.getResponse());
        return testDto;
    }
}
