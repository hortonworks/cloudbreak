package com.sequenceiq.it.cloudbreak.action.v4.credential;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class CredentialDeleteAction implements Action<CredentialTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialDeleteAction.class);

    @Override
    public CredentialTestDto action(TestContext testContext, CredentialTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        Log.when(LOGGER, " Credential delete request, name:" + testDto.getName());
        testDto.setResponse(
                environmentClient.getDefaultClient(testContext)
                        .credentialV1Endpoint()
                        .deleteByName(testDto.getName()));
        Log.whenJson(LOGGER, " Credential deleted successfully:\n", testDto.getResponse());
        return testDto;
    }
}
