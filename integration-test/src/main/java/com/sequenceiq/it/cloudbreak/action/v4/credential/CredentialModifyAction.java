package com.sequenceiq.it.cloudbreak.action.v4.credential;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;

public class CredentialModifyAction implements Action<CredentialTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialModifyAction.class);

    @Override
    public CredentialTestDto action(TestContext testContext, CredentialTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
//        Log.logJSON(LOGGER, " Credential modifyV4 request:\n", testDto.getRequest());
//        testDto.setResponse(
//                cloudbreakClient.getCloudbreakClient()
//                        .credentialV4Endpoint()
//                        .put(cloudbreakClient.getWorkspaceId(), testDto.getRequest()));
//        Log.logJSON(LOGGER, " Credential modified successfully:\n", testDto.getResponse());
//        Log.log(LOGGER, String.format(" ID: %s", testDto.getResponse().getId()));

        return testDto;
    }
}
