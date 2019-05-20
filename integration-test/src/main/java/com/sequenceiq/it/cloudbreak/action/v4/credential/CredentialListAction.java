package com.sequenceiq.it.cloudbreak.action.v4.credential;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class CredentialListAction implements Action<CredentialTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialListAction.class);

    @Override
    public CredentialTestDto action(TestContext testContext, CredentialTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Collection<CredentialV4Response> responses = cloudbreakClient.getCloudbreakClient()
                .credentialV4Endpoint()
                .list(cloudbreakClient.getWorkspaceId())
                .getResponses();
        testDto.setResponses(responses.stream().collect(Collectors.toSet()));
        Log.logJSON(LOGGER, " Credential listed successfully:\n", testDto.getResponses());
        return testDto;
    }
}
