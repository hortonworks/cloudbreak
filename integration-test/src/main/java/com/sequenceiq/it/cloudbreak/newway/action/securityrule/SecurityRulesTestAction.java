package com.sequenceiq.it.cloudbreak.newway.action.securityrule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.securityrule.SecurityRulesTestDto;

public class SecurityRulesTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityRulesTestAction.class);

    private SecurityRulesTestAction() {
    }

    public static SecurityRulesTestDto getDefaultSecurityRules(TestContext testContext, SecurityRulesTestDto entity, CloudbreakClient client) {
        String logInitMessage = "Obtaining default security rules";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(client.getCloudbreakClient().utilV4Endpoint().getDefaultSecurityRules(entity.getKnoxEnabled()));
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}
