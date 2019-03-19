package com.sequenceiq.it.cloudbreak.newway.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.util.SecurityRulesTestDto;

public class SecurityRulesAction implements Action<SecurityRulesTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityRulesAction.class);

    @Override
    public SecurityRulesTestDto action(TestContext testContext, SecurityRulesTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Obtaining default security rules";
        LOGGER.info("{}", logInitMessage);
        testDto.setResponse(cloudbreakClient.getCloudbreakClient().utilV4Endpoint().getDefaultSecurityRules(testDto.getKnoxEnabled()));
        LOGGER.info("{} was successful", logInitMessage);
        return testDto;
    }
}
