package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

class StackScalabilityConditionTest {

    private static final String GROUP_NAME = "worker";

    private StackScalabilityCondition underTest = new StackScalabilityCondition();

    @Test
    void testIsScalableShouldReturnFalseWhenThereArePendingInstances() {
        Set<InstanceMetaData> instanceMetaData = createInstanceMetadataWithPendingInstance();
        Stack stack = createStac(instanceMetaData);

        boolean actual = underTest.isScalable(stack, GROUP_NAME);

        assertFalse(actual);
    }

    @Test
    void testIsScalableShouldReturnTrueWhenThereAreNoPendingInstances() {
        Set<InstanceMetaData> instanceMetaData = createInstanceMetadataWithRegisteredInstances();
        Stack stack = createStac(instanceMetaData);

        boolean actual = underTest.isScalable(stack, GROUP_NAME);

        assertTrue(actual);
    }

    private Stack createStac(Set<InstanceMetaData> instanceMetaData) {
        Stack stack = new Stack();
        stack.setInstanceGroups(Set.of(
                createInstanceGroup("master", Collections.emptySet()),
                createInstanceGroup(GROUP_NAME, instanceMetaData)));
        return stack;
    }

    private InstanceGroup createInstanceGroup(String name, Set<InstanceMetaData> instanceMetadataSet) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(name);
        instanceGroup.setInstanceMetaData(instanceMetadataSet);
        return instanceGroup;
    }

    private Set<InstanceMetaData> createInstanceMetadataWithPendingInstance() {
        return Set.of(
                createInstanceMetaData(InstanceStatus.SERVICES_HEALTHY),
                createInstanceMetaData(InstanceStatus.SERVICES_HEALTHY),
                createInstanceMetaData(InstanceStatus.REQUESTED));
    }

    private Set<InstanceMetaData> createInstanceMetadataWithRegisteredInstances() {
        return Set.of(
                createInstanceMetaData(InstanceStatus.SERVICES_HEALTHY),
                createInstanceMetaData(InstanceStatus.SERVICES_HEALTHY),
                createInstanceMetaData(InstanceStatus.SERVICES_HEALTHY));
    }

    private InstanceMetaData createInstanceMetaData(InstanceStatus instanceStatus) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(instanceStatus);
        return instanceMetaData;
    }

}