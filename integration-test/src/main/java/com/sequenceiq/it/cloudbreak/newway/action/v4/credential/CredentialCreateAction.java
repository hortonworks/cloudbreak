package com.sequenceiq.it.cloudbreak.newway.action.v4.credential;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;

public class CredentialCreateAction implements Action<CredentialTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialCreateAction.class);

    @Override
    public CredentialTestDto action(TestContext testContext, CredentialTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        logJSON(LOGGER, " Credential create request:\n", testDto.getRequest());
        testDto.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .credentialV4Endpoint()
                        .post(cloudbreakClient.getWorkspaceId(), testDto.getRequest()));
        logJSON(LOGGER, " Credential created successfully:\n", testDto.getResponse());
        log(LOGGER, format(" ID: %s", testDto.getResponse().getId()));

        return testDto;
    }
}
