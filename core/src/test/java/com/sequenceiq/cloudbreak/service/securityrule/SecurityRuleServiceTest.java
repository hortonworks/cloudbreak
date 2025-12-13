package com.sequenceiq.cloudbreak.service.securityrule;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SecurityRuleV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SecurityRulesV4Response;

class SecurityRuleServiceTest {

    private SecurityRuleService underTest = new SecurityRuleService();

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(underTest, "gatewayPort", "9443");
        ReflectionTestUtils.setField(underTest, "httpsPort", "443");
        ReflectionTestUtils.setField(underTest, "sshPort", "22");
        ReflectionTestUtils.setField(underTest, "defaultGatewayCidr", Sets.newHashSet("0.0.0.0/0"));
    }

    @Test
    void getDefaultSecurityRulesWhenKnoxIsEnabled() {
        SecurityRulesV4Response defaultSecurityRules = underTest.getDefaultSecurityRules();

        assertEquals(3, defaultSecurityRules.getGateway().size());
        assertEquals(1, defaultSecurityRules.getCore().size());

        assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "9443"));
        assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "443"));
        assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "22"));
        assertTrue(containsServicePort(defaultSecurityRules.getCore(), "22"));
    }

    @Test
    void getDefaultSecurityRulesWhenKnoxIsEnabledAndKnoxPortIsSet() {
        SecurityRulesV4Response defaultSecurityRules = underTest.getDefaultSecurityRules();

        assertEquals(3, defaultSecurityRules.getGateway().size());
        assertEquals(1, defaultSecurityRules.getCore().size());

        assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "9443"));
        assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "443"));
        assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "22"));
        assertTrue(containsServicePort(defaultSecurityRules.getCore(), "22"));
    }

    @Test
    void getDefaultSecurityRulesWhenKnoxIsDisabledAndHttpsPortIsSet() {
        SecurityRulesV4Response defaultSecurityRules = underTest.getDefaultSecurityRules();

        assertEquals(3, defaultSecurityRules.getGateway().size());
        assertEquals(1, defaultSecurityRules.getCore().size());

        assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "9443"));
        assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "443"));
        assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "22"));
        assertTrue(containsServicePort(defaultSecurityRules.getCore(), "22"));
    }

    private boolean containsServicePort(List<SecurityRuleV4Response> securityRulesResponses, String servicePort) {
        return securityRulesResponses.stream().anyMatch(securityRulesResponse -> securityRulesResponse.getPorts().contains(servicePort));
    }

}