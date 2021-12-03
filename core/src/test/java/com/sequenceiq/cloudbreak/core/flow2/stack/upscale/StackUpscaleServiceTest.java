package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static com.sequenceiq.cloudbreak.TestUtil.instanceMetaData;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
class StackUpscaleServiceTest {

    @Mock
    private StackScalabilityCondition stackScalabilityCondition;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private StackUpscaleService underTest;

    @Test
    public void testGetInstanceCountToCreateWhenRepair() {
        InstanceGroup instanceGroup = new InstanceGroup();
        when(stackScalabilityCondition.isScalable(any(), eq("worker"))).thenReturn(Boolean.TRUE);
        when(instanceMetaDataService.unusedInstancesInInstanceGroupByName(eq(1L), eq("worker")))
                .thenReturn(Set.of(instanceMetaData(1L, 1L, InstanceStatus.CREATED, false, instanceGroup),
                        instanceMetaData(2L, 1L, InstanceStatus.CREATED, false, instanceGroup)));

        int instanceCountToCreate = underTest.getInstanceCountToCreate(TestUtil.stack(), "worker", 3, true);
        assertEquals(3, instanceCountToCreate);
    }

    @Test
    public void testGetInstanceCountToCreateWhenUpscale() {
        InstanceGroup instanceGroup = new InstanceGroup();
        when(stackScalabilityCondition.isScalable(any(), eq("worker"))).thenReturn(Boolean.TRUE);
        when(instanceMetaDataService.unusedInstancesInInstanceGroupByName(eq(1L), eq("worker")))
                .thenReturn(Set.of(instanceMetaData(1L, 1L, InstanceStatus.CREATED, false, instanceGroup),
                        instanceMetaData(2L, 1L, InstanceStatus.CREATED, false, instanceGroup)));

        int instanceCountToCreate = underTest.getInstanceCountToCreate(TestUtil.stack(), "worker", 3, false);
        assertEquals(1, instanceCountToCreate);
    }

    @Test
    public void testGetInstanceCountToCreateWhenStackIsNotScalable() {
        InstanceGroup instanceGroup = new InstanceGroup();
        when(stackScalabilityCondition.isScalable(any(), eq("worker"))).thenReturn(Boolean.FALSE);
        when(instanceMetaDataService.unusedInstancesInInstanceGroupByName(eq(1L), eq("worker")))
                .thenReturn(Set.of(instanceMetaData(1L, 1L, InstanceStatus.CREATED, false, instanceGroup),
                        instanceMetaData(2L, 1L, InstanceStatus.CREATED, false, instanceGroup)));

        int instanceCountToCreate = underTest.getInstanceCountToCreate(TestUtil.stack(), "worker", 3, false);
        assertEquals(0, instanceCountToCreate);
    }
}