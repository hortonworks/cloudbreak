package com.sequenceiq.it.cloudbreak.action.v4.credential;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.exception.ProxyMethodInvocationException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class CredentialCreateIfNotExistAction implements Action<CredentialTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialCreateIfNotExistAction.class);

    @Override
    public CredentialTestDto action(TestContext testContext, CredentialTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        Log.when(LOGGER, "Credential create request: " + testDto.getRequest());
        try {
            testDto.setResponse(
                    environmentClient.getDefaultClient(testContext).credentialV1Endpoint().create(testDto.getRequest())
            );
            Log.whenJson(LOGGER, "Credential created successfully: ", testDto.getRequest());
        } catch (ProxyMethodInvocationException e) {
            Log.when(LOGGER, "Cannot create Credential, fetch existed one: " + testDto.getRequest());

            testDto.setResponse(
                    environmentClient.getDefaultClient(testContext).credentialV1Endpoint()
                            .getByName(testDto.getRequest().getName()));
        }
        if (testDto.getResponse() == null) {
            throw new IllegalStateException("Credential could not be created, or could not got with name." + testDto.getName());
        }
        return testDto;
    }
}
