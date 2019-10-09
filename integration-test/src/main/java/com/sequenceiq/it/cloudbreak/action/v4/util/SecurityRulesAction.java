package com.sequenceiq.it.cloudbreak.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.securityrule.SecurityRulesTestDto;

public class SecurityRulesAction implements Action<SecurityRulesTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityRulesAction.class);

    @Override
    public SecurityRulesTestDto action(TestContext testContext, SecurityRulesTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Obtaining default security rules";
        LOGGER.info("{}", logInitMessage);
        testDto.setResponse(cloudbreakClient.getCloudbreakClient().utilV4Endpoint().getDefaultSecurityRules());
        LOGGER.info("{} was successful", logInitMessage);
        return testDto;
    }
}
