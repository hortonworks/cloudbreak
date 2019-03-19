package com.sequenceiq.it.cloudbreak.newway.action.v4.credential;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;

public class CredentialListAction implements Action<CredentialTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialListAction.class);

    @Override
    public CredentialTestDto action(TestContext testContext, CredentialTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Collection<CredentialV4Response> responses = cloudbreakClient.getCloudbreakClient()
                .credentialV4Endpoint()
                .list(cloudbreakClient.getWorkspaceId())
                .getResponses();
        testDto.setResponses(responses.stream().collect(Collectors.toSet()));
        logJSON(LOGGER, " Credential listed successfully:\n", testDto.getResponses());
        return testDto;
    }
}
