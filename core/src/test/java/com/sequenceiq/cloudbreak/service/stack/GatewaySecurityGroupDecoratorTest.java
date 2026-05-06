package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.Tunnel;

class GatewaySecurityGroupDecoratorTest {

    private GatewaySecurityGroupDecorator underTest;

    private Stack stack;

    @BeforeEach
    void init() {
        underTest = new GatewaySecurityGroupDecorator();
        stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        instanceGroup.setGroupName("GWIG");
        stack.setInstanceGroups(Set.of(instanceGroup));
    }

    @Test
    void testConvertExtendsGatewaySecurityGroupsWithDefaultGatewayCidrsWithoutCCM() {
        ReflectionTestUtils.setField(underTest, "defaultGatewayCidr", Set.of("0.0.0.0/0", "1.1.1.1/1"));
        ReflectionTestUtils.setField(underTest, "nginxPort", 9443);
        ReflectionTestUtils.setField(underTest, "httpsPort", 443);

        // WHEN
        underTest.extendGatewaySecurityGroupWithDefaultGatewayCidrs(stack, Tunnel.DIRECT);
        // THEN
        Set<InstanceGroup> gateways =
                stack.getInstanceGroups().stream().filter(ig -> InstanceGroupType.isGateway(ig.getInstanceGroupType())).collect(Collectors.toSet());
        for (InstanceGroup ig : gateways) {
            assertEquals(2, ig.getSecurityGroup().getSecurityRules().stream()
                    .filter(rule -> rule.getCidr().equals("0.0.0.0/0")
                            || rule.getCidr().equals("1.1.1.1/1")
                            && Arrays.stream(rule.getPorts()).anyMatch(port -> port.equals("9443"))).count());
        }
    }

    @Test
    void testConvertDoesntExtendGatewaySecurityGroupsWithDefaultGatewayCidrsIfItsEmptyWithoutCCM() {
        ReflectionTestUtils.setField(underTest, "defaultGatewayCidr", Set.of());
        // WHEN
        underTest.extendGatewaySecurityGroupWithDefaultGatewayCidrs(stack, Tunnel.DIRECT);
        // THEN
        Set<InstanceGroup> gateways =
                stack.getInstanceGroups().stream().filter(ig -> InstanceGroupType.isGateway(ig.getInstanceGroupType())).collect(Collectors.toSet());
        for (InstanceGroup ig : gateways) {
            assertNull(ig.getSecurityGroup());
        }
    }

    @Test
    void testConvertDoesntExtendsGatewaySecurityGroupsWithDefaultGatewayCidrsWithCCM() {
        ReflectionTestUtils.setField(underTest, "defaultGatewayCidr", Set.of("0.0.0.0/0", "1.1.1.1/1"));
        ReflectionTestUtils.setField(underTest, "nginxPort", 9443);

        // WHEN
        underTest.extendGatewaySecurityGroupWithDefaultGatewayCidrs(stack, Tunnel.CCM);
        // THEN
        Set<InstanceGroup> gateways =
                stack.getInstanceGroups().stream().filter(ig -> InstanceGroupType.isGateway(ig.getInstanceGroupType())).collect(Collectors.toSet());
        for (InstanceGroup ig : gateways) {
            assertNull(ig.getSecurityGroup());
        }
    }

    @Test
    void testConvertDoesntExtendGatewaySecurityGroupsWithDefaultGatewayCidrsIfItsEmptyWithCCM() {
        ReflectionTestUtils.setField(underTest, "defaultGatewayCidr", Set.of());
        // WHEN
        underTest.extendGatewaySecurityGroupWithDefaultGatewayCidrs(stack, Tunnel.CCM);
        // THEN
        Set<InstanceGroup> gateways =
                stack.getInstanceGroups().stream().filter(ig -> InstanceGroupType.isGateway(ig.getInstanceGroupType())).collect(Collectors.toSet());
        for (InstanceGroup ig : gateways) {
            assertNull(ig.getSecurityGroup());
        }
    }

    @Test
    void testNoRulesAddedWhenAllPortsAlreadyExist() {
        ReflectionTestUtils.setField(underTest, "defaultGatewayCidr", Set.of("10.0.0.0/8"));
        ReflectionTestUtils.setField(underTest, "nginxPort", 9443);
        ReflectionTestUtils.setField(underTest, "httpsPort", 443);

        InstanceGroup gateway = stack.getInstanceGroups().iterator().next();
        SecurityGroup securityGroup = new SecurityGroup();
        SecurityRule existingRule = new SecurityRule();
        existingRule.setPorts("9443,443");
        existingRule.setProtocol("tcp");
        existingRule.setCidr("192.168.0.0/16");
        existingRule.setSecurityGroup(securityGroup);
        securityGroup.setSecurityRules(new HashSet<>(Set.of(existingRule)));
        gateway.setSecurityGroup(securityGroup);

        underTest.extendGatewaySecurityGroupWithDefaultGatewayCidrs(stack, Tunnel.DIRECT);

        assertEquals(1, securityGroup.getSecurityRules().size());
    }

    @Test
    void testOnlyMissingPortAddedWhenGatewayPortAlreadyExists() {
        ReflectionTestUtils.setField(underTest, "defaultGatewayCidr", Set.of("10.0.0.0/8"));
        ReflectionTestUtils.setField(underTest, "nginxPort", 9443);
        ReflectionTestUtils.setField(underTest, "httpsPort", 443);

        InstanceGroup gateway = stack.getInstanceGroups().iterator().next();
        SecurityGroup securityGroup = new SecurityGroup();
        SecurityRule existingRule = new SecurityRule();
        existingRule.setPorts("9443");
        existingRule.setProtocol("tcp");
        existingRule.setCidr("192.168.0.0/16");
        existingRule.setSecurityGroup(securityGroup);
        securityGroup.setSecurityRules(new HashSet<>(Set.of(existingRule)));
        gateway.setSecurityGroup(securityGroup);

        underTest.extendGatewaySecurityGroupWithDefaultGatewayCidrs(stack, Tunnel.DIRECT);

        assertEquals(2, securityGroup.getSecurityRules().size());
        Set<String> newRulePorts = securityGroup.getSecurityRules().stream()
                .filter(r -> "10.0.0.0/8".equals(r.getCidr()))
                .flatMap(r -> Arrays.stream(r.getPorts()))
                .collect(Collectors.toSet());
        assertTrue(newRulePorts.contains("443"));
        assertEquals(1, newRulePorts.size());
    }

}