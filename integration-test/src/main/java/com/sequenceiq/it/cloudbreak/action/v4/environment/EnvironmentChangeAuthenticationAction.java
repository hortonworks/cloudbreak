package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAuthenticationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentChangeAuthenticationAction implements Action<EnvironmentTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentChangeAuthenticationAction.class);

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        EnvironmentEditRequest request = new EnvironmentEditRequest();
        EnvironmentAuthenticationRequest environmentAuthenticationRequest = new EnvironmentAuthenticationRequest();
        environmentAuthenticationRequest.setPublicKey(testDto.getRequest().getAuthentication().getPublicKey());
        environmentAuthenticationRequest.setPublicKeyId(testDto.getRequest().getAuthentication().getPublicKeyId());
        request.setAuthentication(environmentAuthenticationRequest);
        testDto.setResponse(environmentClient.getDefaultClient(testContext)
                .environmentV1Endpoint()
                .editByCrn(testDto.getResponse().getCrn(), request));
        Log.when(LOGGER, "Environment edit authentication action posted");
        return testDto;
    }
}
