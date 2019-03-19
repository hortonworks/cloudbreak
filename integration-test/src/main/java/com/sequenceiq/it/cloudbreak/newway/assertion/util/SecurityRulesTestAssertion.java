package com.sequenceiq.it.cloudbreak.newway.assertion.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.entity.util.SecurityRulesTestDto;

public class SecurityRulesTestAssertion {

    private SecurityRulesTestAssertion() {
    }

    public static AssertionV2<SecurityRulesTestDto> coreIsNotEmpty() {
        return (testContext, entity, cloudbreakClient) -> {
            assertNotNull(entity.getResponse().getCore());
            assertFalse(entity.getResponse().getCore().isEmpty());
            return entity;
        };
    }

    public static AssertionV2<SecurityRulesTestDto> gatewayIsNotEmpty() {
        return (testContext, entity, cloudbreakClient) -> {
            assertNotNull(entity.getResponse().getGateway());
            assertFalse(entity.getResponse().getGateway().isEmpty());
            return entity;
        };
    }

}
