package com.sequenceiq.it.cloudbreak.newway.assertion.securityrule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.securityrule.SecurityRulesTestDto;

public class SecurityRulesAssertions {

    private SecurityRulesAssertions() {
    }

    public static SecurityRulesTestDto coreIsNotEmpty(TestContext tc, SecurityRulesTestDto entity, CloudbreakClient cc) {
        assertNotNull(entity.getResponse().getCore());
        assertFalse(entity.getResponse().getCore().isEmpty());
        return entity;
    }

    public static SecurityRulesTestDto gatewayIsNotEmpty(TestContext tc, SecurityRulesTestDto entity, CloudbreakClient cc) {
        assertNotNull(entity.getResponse().getGateway());
        assertFalse(entity.getResponse().getGateway().isEmpty());
        return entity;
    }

}
