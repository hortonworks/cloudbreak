package com.sequenceiq.cloudbreak.domain;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.common.api.type.InstanceGroupType;

public class StackTest {

    @Test
    public void testGetGatewayGroup() {
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

}
