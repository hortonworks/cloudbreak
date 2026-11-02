package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sequenceiq.common.api.type.LoadBalancerType;

class AzureLoadBalancerResourceNamesTest {

    private static final String LB_NAME = "LoadBalancerstackPRIVATE";

    @Test
    void testRuleName() {
        assertEquals("port-443TCP-rule", AzureLoadBalancerResourceNames.ruleName(443, "TCP"));
        assertEquals("port-443-rule", AzureLoadBalancerResourceNames.ruleName(443, ""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"port-443TCP-rule", "port-443UDP-rule", "port-443TCP_UDP-rule", "port-443HTTPS-rule", "port-443-rule",
            "port-443TCP-rule-gateway", "port-443-rule-gateway"})
    void testIsCloudbreakRuleMatchesCloudbreakNames(String ruleName) {
        assertTrue(AzureLoadBalancerResourceNames.isCloudbreakRule(ruleName, 443));
    }

    @ParameterizedTest
    @ValueSource(strings = {"aaa-foreign-443", "port-21000", "port-21000TCP-rule", "port-4430TCP-rule", "port-44TCP-rule", "port-443tcp-rule",
            "port-443TCP-rule-custom", "xport-443TCP-rule", "proxy-pool", ""})
    void testIsCloudbreakRuleRejectsOtherNames(String ruleName) {
        assertFalse(AzureLoadBalancerResourceNames.isCloudbreakRule(ruleName, 443));
    }

    @Test
    void testIsCloudbreakRuleRejectsNull() {
        assertFalse(AzureLoadBalancerResourceNames.isCloudbreakRule(null, 443));
    }

    @ParameterizedTest
    @CsvSource({
            "master-pool, master",
            "address-pool, address",
            "proxy-pool, proxy",
            "my-pool-group-pool, my-pool-group",
            "pool, pool",
            "custom, custom"
    })
    void testGroupNameRemovesOnlyThePoolSuffix(String poolName, String expectedGroupName) {
        assertEquals(expectedGroupName, AzureLoadBalancerResourceNames.groupName(poolName));
    }

    @Test
    void testGroupNameKeepsNull() {
        assertNull(AzureLoadBalancerResourceNames.groupName(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"port-443TCP-rule-gateway", "port-443-rule-gateway", "custom-gateway"})
    void testIsGatewayRuleMatchesGatewaySuffix(String ruleName) {
        assertTrue(AzureLoadBalancerResourceNames.isGatewayRule(ruleName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"port-443TCP-rule", "gateway-rule", "customgateway", ""})
    void testIsGatewayRuleRejectsOtherNames(String ruleName) {
        assertFalse(AzureLoadBalancerResourceNames.isGatewayRule(ruleName));
    }

    @Test
    void testIsGatewayRuleRejectsNull() {
        assertFalse(AzureLoadBalancerResourceNames.isGatewayRule(null));
    }

    @Test
    void testFrontendName() {
        assertEquals(LB_NAME + "-frontend", AzureLoadBalancerResourceNames.frontendName(LB_NAME, LoadBalancerType.PRIVATE));
        assertEquals(LB_NAME + "-frontend", AzureLoadBalancerResourceNames.frontendName(LB_NAME, LoadBalancerType.PUBLIC));
        assertEquals(LB_NAME + "-frontend-gateway", AzureLoadBalancerResourceNames.frontendName(LB_NAME, LoadBalancerType.GATEWAY_PRIVATE));
    }
}
