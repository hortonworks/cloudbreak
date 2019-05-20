package com.sequenceiq.it.cloudbreak.assertion.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.securityrule.SecurityRulesTestDto;

public class SecurityRulesTestAssertion {

    private SecurityRulesTestAssertion() {
    }

    public static Assertion<SecurityRulesTestDto> coreIsNotEmpty() {
        return (testContext, entity, cloudbreakClient) -> {
            assertNotNull(entity.getResponse().getCore());
            assertFalse(entity.getResponse().getCore().isEmpty());
            return entity;
        };
    }

    public static Assertion<SecurityRulesTestDto> gatewayIsNotEmpty() {
        return (testContext, entity, cloudbreakClient) -> {
            assertNotNull(entity.getResponse().getGateway());
            assertFalse(entity.getResponse().getGateway().isEmpty());
            return entity;
        };
    }

}
