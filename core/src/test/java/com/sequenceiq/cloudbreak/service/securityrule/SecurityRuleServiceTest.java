package com.sequenceiq.cloudbreak.service.securityrule;


import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.SecurityRuleResponse;
import com.sequenceiq.cloudbreak.api.model.SecurityRulesResponse;

@RunWith(MockitoJUnitRunner.class)
public class SecurityRuleServiceTest {

    @InjectMocks
    private SecurityRuleService underTest;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(underTest, "gatewayPort", "9443");
        ReflectionTestUtils.setField(underTest, "knoxPort", "8443");
        ReflectionTestUtils.setField(underTest, "sshPort", "22");
        ReflectionTestUtils.setField(underTest, "defaultGatewayCidr", Sets.newHashSet("0.0.0.0/0"));
    }

    @Test
    public void getDefaultSecurityRulesWhenKnoxIsEnabled() {
        SecurityRulesResponse defaultSecurityRules = underTest.getDefaultSecurityRules(true);

        Assert.assertEquals(3, defaultSecurityRules.getGateway().size());
        Assert.assertEquals(1, defaultSecurityRules.getCore().size());

        Assert.assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "9443"));
        Assert.assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "8443"));
        Assert.assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "22"));
        Assert.assertTrue(containsServicePort(defaultSecurityRules.getCore(), "22"));
    }

    @Test
    public void getDefaultSecurityRulesWhenKnoxIsDisabled() {
        SecurityRulesResponse defaultSecurityRules = underTest.getDefaultSecurityRules(false);
        Assert.assertEquals(2, defaultSecurityRules.getGateway().size());
        Assert.assertEquals(1, defaultSecurityRules.getCore().size());

        Assert.assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "9443"));
        Assert.assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "22"));
        Assert.assertTrue(containsServicePort(defaultSecurityRules.getCore(), "22"));
    }

    private boolean containsServicePort(List<SecurityRuleResponse> securityRulesResponses, String servicePort) {
        return securityRulesResponses.stream().anyMatch(securityRulesResponse -> securityRulesResponse.getPorts().contains(servicePort));
    }
}