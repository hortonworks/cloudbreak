package com.sequenceiq.it.cloudbreak.action.v4.credential;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class CredentialCreateAction implements Action<CredentialTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialCreateAction.class);

    @Override
    public CredentialTestDto action(TestContext testContext, CredentialTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        LOGGER.info(" Credential create request: {}", testDto.getRequest());
//        testDto.setResponse(
//                cloudbreakClient.getCloudbreakClient()
//                        .credentialV4Endpoint()
//                        .post(cloudbreakClient.getWorkspaceId(), testDto.getRequest()));
//        Log.logJSON(LOGGER, " Credential created successfully:\n", testDto.getResponse());
//        Log.log(LOGGER, format(" ID: %s", testDto.getResponse().getId()));

        return testDto;
    }
}
