package com.sequenceiq.cloudbreak.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.common.api.type.InstanceGroupType;

class StackTest {

    @Test
    void testGetGatewayGroup() {
        InstanceGroup master = new InstanceGroup();
        InstanceGroup data = new InstanceGroup();
        InstanceGroup compute = new InstanceGroup();
        InstanceGroup gateway = new InstanceGroup();
        gateway.setInstanceGroupType(InstanceGroupType.GATEWAY);

        master.setGroupName("master");
        data.setGroupName("data");
        compute.setGroupName("compute");
        gateway.setGroupName("gateway");

        Stack stack = new Stack();
        stack.setInstanceGroups(Set.of(master, data, compute, gateway));

        assertEquals(4, stack.getInstanceGroups().size());
        assertEquals("gateway", stack.getGatewayHostGroup().get().getGroupName());
    }

    @Test
    void testPopulateStackIdForComponents() {
        Stack stack = new Stack();
        Long stackId = 100L;
        stack.setId(stackId);
        Set<Component> components = Set.of(new Component(), new Component());
        stack.getComponents().addAll(components);
        stack.populateStackIdForComponents();
        stack.getComponents().forEach(component -> assertEquals(stackId, component.getStackId()));
    }

}
