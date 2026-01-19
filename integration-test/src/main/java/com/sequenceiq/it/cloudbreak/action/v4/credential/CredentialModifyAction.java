package com.sequenceiq.it.cloudbreak.action.v4.credential;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class CredentialModifyAction implements Action<CredentialTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialModifyAction.class);

    @Override
    public CredentialTestDto action(TestContext testContext, CredentialTestDto testDto, EnvironmentClient cloudbreakClient) throws Exception {
        Log.whenJson(LOGGER, " Credential modifyV4 request:\n", testDto.getRequest());
        testDto.setResponse(
                cloudbreakClient.getDefaultClient(testContext)
                        .credentialV1Endpoint()
                        .modify(testDto.modifyRequest()));
        Log.whenJson(LOGGER, " Credential modified successfully:\n", testDto.getResponse());
        Log.when(LOGGER, String.format(" CRN: %s", testDto.getResponse().getCrn()));

        return testDto;
    }
}
