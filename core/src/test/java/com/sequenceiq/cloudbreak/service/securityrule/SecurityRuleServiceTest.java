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
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SecurityRuleV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SecurityRulesV4Response;

@RunWith(MockitoJUnitRunner.class)
public class SecurityRuleServiceTest {

    @InjectMocks
    private SecurityRuleService underTest;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(underTest, "gatewayPort", "9443");
        ReflectionTestUtils.setField(underTest, "httpsPort", "443");
        ReflectionTestUtils.setField(underTest, "sshPort", "22");
        ReflectionTestUtils.setField(underTest, "defaultGatewayCidr", Sets.newHashSet("0.0.0.0/0"));
    }

    @Test
    public void getDefaultSecurityRulesWhenKnoxIsEnabled() {
        SecurityRulesV4Response defaultSecurityRules = underTest.getDefaultSecurityRules();

        Assert.assertEquals(3, defaultSecurityRules.getGateway().size());
        Assert.assertEquals(1, defaultSecurityRules.getCore().size());

        Assert.assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "9443"));
        Assert.assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "443"));
        Assert.assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "22"));
        Assert.assertTrue(containsServicePort(defaultSecurityRules.getCore(), "22"));
    }

    @Test
    public void getDefaultSecurityRulesWhenKnoxIsEnabledAndKnoxPortIsSet() {
        SecurityRulesV4Response defaultSecurityRules = underTest.getDefaultSecurityRules();

        Assert.assertEquals(3, defaultSecurityRules.getGateway().size());
        Assert.assertEquals(1, defaultSecurityRules.getCore().size());

        Assert.assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "9443"));
        Assert.assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "443"));
        Assert.assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "22"));
        Assert.assertTrue(containsServicePort(defaultSecurityRules.getCore(), "22"));
    }

    @Test
    public void getDefaultSecurityRulesWhenKnoxIsDisabledAndHttpsPortIsSet() {
        SecurityRulesV4Response defaultSecurityRules = underTest.getDefaultSecurityRules();

        Assert.assertEquals(3, defaultSecurityRules.getGateway().size());
        Assert.assertEquals(1, defaultSecurityRules.getCore().size());

        Assert.assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "9443"));
        Assert.assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "443"));
        Assert.assertTrue(containsServicePort(defaultSecurityRules.getGateway(), "22"));
        Assert.assertTrue(containsServicePort(defaultSecurityRules.getCore(), "22"));
    }

    private boolean containsServicePort(List<SecurityRuleV4Response> securityRulesResponses, String servicePort) {
        return securityRulesResponses.stream().anyMatch(securityRulesResponse -> securityRulesResponse.getPorts().contains(servicePort));
    }

}