package com.sequenceiq.it.cloudbreak.action.v4.credential;

import java.util.Collection;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class CredentialListAction implements Action<CredentialTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialListAction.class);

    @Override
    public CredentialTestDto action(TestContext testContext, CredentialTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        Collection<CredentialResponse> responses = environmentClient.getDefaultClient(testContext)
                .credentialV1Endpoint()
                .list()
                .getResponses();
        testDto.setResponses(new HashSet<>(responses));
        Log.whenJson(LOGGER, " Credential listed successfully:\n", testDto.getResponses());
        return testDto;
    }
}
