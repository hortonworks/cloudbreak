package com.sequenceiq.it.cloudbreak.action.v4.credential;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.EnvironmentServiceClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class CredentialListAction implements Action<CredentialTestDto, EnvironmentServiceClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialListAction.class);

    @Override
    public CredentialTestDto action(TestContext testContext, CredentialTestDto testDto, EnvironmentServiceClient client) throws Exception {
        Log.log(LOGGER, format(" Credential list action: %s"));
        testDto.setCredentialResponses(client.getEnvironmentServiceClient()
                .credentialV1Endpoint()
                .list());
        Log.logJSON(LOGGER, format(" Credentials listed successfully. Crn: :%n"), testDto.getCredentialResponses());
        return testDto;
    }
}
