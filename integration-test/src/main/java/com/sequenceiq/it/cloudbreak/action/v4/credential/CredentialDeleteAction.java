package com.sequenceiq.it.cloudbreak.action.v4.credential;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.EnvironmentServiceClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;

public class CredentialDeleteAction implements Action<CredentialTestDto, EnvironmentServiceClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialDeleteAction.class);

    @Override
    public CredentialTestDto action(TestContext testContext, CredentialTestDto testDto, EnvironmentServiceClient client) throws Exception {
        LOGGER.info(String.format(" Name: %s", testDto.getRequest().getName()));
//        Log.logJSON(LOGGER, " Credential delete request:\n", testDto.getRequest());
//        testDto.setResponse(
//                cloudbreakClient.getCloudbreakClient()
//                        .credentialV4Endpoint()
//                        .delete(cloudbreakClient.getWorkspaceId(), testDto.getName()));
//        Log.logJSON(LOGGER, " Credential deleted successfully:\n", testDto.getResponse());
        return testDto;
    }
}
