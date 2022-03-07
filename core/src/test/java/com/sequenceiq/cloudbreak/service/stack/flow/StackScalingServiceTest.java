package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@ExtendWith(MockitoExtension.class)
public class StackScalingServiceTest {

    @InjectMocks
    private StackScalingService stackScalingService;

    @Test
    public void testGetUnusedInstanceIds() {
        Stack stack = mock(Stack.class);
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        when(stack.getInstanceGroupByInstanceGroupName("worker")).thenReturn(instanceGroup);
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setStartDate(1L);
        instanceMetaData1.setPrivateId(1L);
        instanceMetaData1.setInstanceId("instance-id-1");
        instanceMetaData1.setDiscoveryFQDN("fqdn1");
        instanceMetaDataSet.add(instanceMetaData1);
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setStartDate(3L);
        instanceMetaData2.setPrivateId(3L);
        instanceMetaData2.setInstanceId("instance-id-2");
        instanceMetaData2.setDiscoveryFQDN("fqdn2");
        instanceMetaDataSet.add(instanceMetaData2);
        InstanceMetaData instanceMetaData3 = new InstanceMetaData();
        instanceMetaData3.setStartDate(2L);
        instanceMetaData3.setPrivateId(2L);
        instanceMetaData3.setInstanceId("instance-id-3");
        instanceMetaData3.setDiscoveryFQDN("fqdn3");
        instanceMetaDataSet.add(instanceMetaData3);
        InstanceMetaData instanceMetaData4 = new InstanceMetaData();
        instanceMetaData4.setStartDate(4L);
        instanceMetaData4.setPrivateId(4L);
        instanceMetaData4.setInstanceId("instance-id-4");
        instanceMetaData4.setDiscoveryFQDN("fqdn4");
        instanceMetaDataSet.add(instanceMetaData4);
        instanceMetaDataSet.add(new InstanceMetaData());
        when(instanceGroup.getUnattachedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);

        Set<Long> unusedPrivateIds = stackScalingService.getUnusedPrivateIds("worker", -3, stack);
        assertTrue(unusedPrivateIds.contains(1L));
        assertTrue(unusedPrivateIds.contains(2L));
        assertTrue(unusedPrivateIds.contains(3L));
        assertFalse(unusedPrivateIds.contains(4L));
    }

    @Test
    public void testGetUnusedInstanceIdsButWithPositiveScalingAdjustment() {
        Stack stack = mock(Stack.class);
        Set<Long> unusedPrivateIds = stackScalingService.getUnusedPrivateIds("worker", 5, stack);
        assertEquals(0, unusedPrivateIds.size());
    }

}