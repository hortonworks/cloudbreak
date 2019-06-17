package com.sequenceiq.it.cloudbreak.action.v4.credential;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.EnvironmentServiceClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class CredentialCreateAction implements Action<CredentialTestDto, EnvironmentServiceClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialCreateAction.class);

    @Override
    public CredentialTestDto action(TestContext testContext, CredentialTestDto testDto, EnvironmentServiceClient client) throws Exception {
        Log.log(LOGGER, String.format(" Name: %s", testDto.getRequest().getName()));
        Log.logJSON(LOGGER, format(" Credential create :%n"), testDto.getRequest());
        testDto.setResponse(client.getEnvironmentServiceClient()
                .credentialV1Endpoint()
                .post(testDto.getRequest()));
        Log.logJSON(LOGGER, format(" Credential created successfully. Crn: :%n"), testDto.getResponse().getCrn());
        return testDto;
    }
}
