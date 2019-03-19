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

public class CredentialGetAction implements Action<CredentialTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialGetAction.class);

    @Override
    public CredentialTestDto action(TestContext testContext, CredentialTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        log(LOGGER, format(" Name: %s", testDto.getRequest().getName()));
        logJSON(LOGGER, " Credential get request:\n", testDto.getRequest());
        testDto.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .credentialV4Endpoint()
                        .get(cloudbreakClient.getWorkspaceId(), testDto.getName()));
        logJSON(LOGGER, " Credential get successfully:\n", testDto.getResponse());
        return testDto;
    }
}
