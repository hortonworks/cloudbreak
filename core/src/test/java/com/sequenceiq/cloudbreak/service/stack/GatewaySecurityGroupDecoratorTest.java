package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

}