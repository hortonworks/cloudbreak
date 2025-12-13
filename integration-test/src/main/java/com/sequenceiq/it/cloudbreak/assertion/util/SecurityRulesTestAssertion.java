package com.sequenceiq.it.cloudbreak.assertion.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.securityrule.SecurityRulesTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class SecurityRulesTestAssertion {

    private SecurityRulesTestAssertion() {
    }

    public static Assertion<SecurityRulesTestDto, CloudbreakClient> coreIsNotEmpty() {
        return (testContext, entity, cloudbreakClient) -> {
            assertNotNull(entity.getResponse().getCore());
            assertFalse(entity.getResponse().getCore().isEmpty());
            return entity;
        };
    }

    public static Assertion<SecurityRulesTestDto, CloudbreakClient> gatewayIsNotEmpty() {
        return (testContext, entity, cloudbreakClient) -> {
            assertNotNull(entity.getResponse().getGateway());
            assertFalse(entity.getResponse().getGateway().isEmpty());
            return entity;
        };
    }

}
