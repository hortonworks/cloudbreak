package com.sequenceiq.it.cloudbreak.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.securityrule.SecurityRulesTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class SecurityRulesAction implements Action<SecurityRulesTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityRulesAction.class);

    @Override
    public SecurityRulesTestDto action(TestContext testContext, SecurityRulesTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        testDto.setResponse(cloudbreakClient.getDefaultClient(testContext).utilV4Endpoint().getDefaultSecurityRules());
        Log.whenJson(LOGGER, "default security rules response:\n", testDto.getResponse());
        return testDto;
    }
}
