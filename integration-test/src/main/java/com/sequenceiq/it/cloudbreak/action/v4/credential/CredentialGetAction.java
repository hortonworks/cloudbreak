package com.sequenceiq.it.cloudbreak.action.v4.credential;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;

public class CredentialGetAction implements Action<CredentialTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialGetAction.class);

    @Override
    public CredentialTestDto action(TestContext testContext, CredentialTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        LOGGER.info(String.format(" Name: %s", testDto.getRequest().getName()));
//        Log.logJSON(LOGGER, " Credential get request:\n", testDto.getRequest());
//        testDto.setResponse(
//                cloudbreakClient.getCloudbreakClient()
//                        .credentialV4Endpoint()
//                        .get(cloudbreakClient.getWorkspaceId(), testDto.getName()));
//        Log.logJSON(LOGGER, " Credential get successfully:\n", testDto.getResponse());
        return testDto;
    }
}
