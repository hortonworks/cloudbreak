package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.SecurityAccessRequest;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentChangeSecurityAccessAction implements Action<EnvironmentTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentChangeSecurityAccessAction.class);

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        EnvironmentEditRequest request = new EnvironmentEditRequest();
        SecurityAccessRequest securityAccess = testDto.getRequest().getSecurityAccess();
        SecurityAccessRequest clone = cloneSecurityAccessRequest(securityAccess);
        request.setSecurityAccess(clone);
        testDto.setResponse(environmentClient.getDefaultClient(testContext)
                .environmentV1Endpoint()
                .editByCrn(testDto.getResponse().getCrn(), request));
        Log.when(LOGGER, "Environment edit authentication action posted");
        return testDto;
    }

    private SecurityAccessRequest cloneSecurityAccessRequest(SecurityAccessRequest securityAccess) {
        SecurityAccessRequest securityAccessRequest = new SecurityAccessRequest();
        securityAccessRequest.setDefaultSecurityGroupId(securityAccess.getDefaultSecurityGroupId());
        securityAccessRequest.setSecurityGroupIdForKnox(securityAccess.getSecurityGroupIdForKnox());
        securityAccessRequest.setCidr(securityAccess.getCidr());
        return securityAccessRequest;
    }
}
