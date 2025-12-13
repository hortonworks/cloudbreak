package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class ClusterDownscaleServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String HOST_GROUP_NAME = "worker";

    private static final Set<Long> PRIVATE_IDS = Set.of(1L, 2L);

    @Mock
    private StackService stackService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private ClusterService clusterService;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private ClusterDownscaleDetails details;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private ClusterDownscaleService underTest;

    @Test
    void testClusterDownscaleStartedWhenScalingAdjustmentIsGivenAndItIsPositiveThenInstanceGroupEventWillBeCalledThisNumber() {
        underTest.clusterDownscaleStarted(STACK_ID, new ClusterDownscaleTriggerEvent(null, STACK_ID, Map.of(HOST_GROUP_NAME, 1),
                Map.of(HOST_GROUP_NAME, PRIVATE_IDS), Map.of(), null, details));

        verify(flowMessageService, times(1)).fireEventAndLog(STACK_ID,
                Status.UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_REMOVING_NODES, "1");
        verify(flowMessageService, times(1)).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(),
                ResourceEvent.CLUSTER_SCALING_DOWN, "worker");
        verify(clusterService, times(1)).updateClusterStatusByStackId(STACK_ID, DetailedStackStatus.DOWNSCALE_IN_PROGRESS);
        verify(stackDtoService, never()).getById(anyLong());
        verify(stackDtoService, never()).getById(STACK_ID);
        verify(flowMessageService, never()).fireInstanceGroupEventAndLog(eq(STACK_ID), anyString(), anyString(), any(ResourceEvent.class), anyString());
    }

    @Test
    void testClusterDownscaleStartedWhenScalingAdjustmentIsGivenAndItIsNegativeThenInstanceGroupEventWillBeCalledWithTheAbsoluteValueOfThisNumber() {
        underTest.clusterDownscaleStarted(STACK_ID, new ClusterDownscaleTriggerEvent(null, STACK_ID, Map.of(HOST_GROUP_NAME, -1),
                Map.of(HOST_GROUP_NAME, PRIVATE_IDS), Map.of(), null, details));

        verify(flowMessageService, times(1)).fireEventAndLog(STACK_ID,
                Status.UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_REMOVING_NODES, "1");
        verify(flowMessageService, times(1)).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(),
                ResourceEvent.CLUSTER_SCALING_DOWN, "worker");
        verify(clusterService, times(1)).updateClusterStatusByStackId(STACK_ID, DetailedStackStatus.DOWNSCALE_IN_PROGRESS);
        verify(stackDtoService, never()).getById(anyLong());
        verify(stackDtoService, never()).getById(STACK_ID);
        verify(flowMessageService, never()).fireInstanceGroupEventAndLog(eq(STACK_ID), anyString(), anyString(), any(ResourceEvent.class), anyString());
    }

    @Test
    void testClusterDownscaleStartedWhenScalingAdjustmentIsNullAndItIsNegativeThenInstanceGroupEventWillBeCalledWithTheAbsoluteValueOfThisNumber() {
        when(instanceMetaDataService.getAllAvailableHostNamesByPrivateIds(anyLong(), any())).thenReturn(List.of("host1"));

        underTest.clusterDownscaleStarted(STACK_ID, new ClusterDownscaleTriggerEvent(null, STACK_ID, null,
                Map.of(HOST_GROUP_NAME, PRIVATE_IDS), Map.of(), null, details));

        verify(flowMessageService, never()).fireEventAndLog(STACK_ID,
                Status.UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_REMOVING_NODES, "1");
        verify(flowMessageService, times(1)).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(),
                ResourceEvent.CLUSTER_SCALING_DOWN, "worker");
        verify(clusterService, times(1)).updateClusterStatusByStackId(STACK_ID, DetailedStackStatus.DOWNSCALE_IN_PROGRESS);
        verify(stackDtoService, never()).getById(anyLong());
        verify(stackDtoService, never()).getById(STACK_ID);
        verify(flowMessageService, never()).fireInstanceGroupEventAndLog(eq(STACK_ID), anyString(), anyString(), any(ResourceEvent.class), anyString());
    }

    @Test
    void testClusterDownscaleStartedWhenZombieHostgroupsIsGivenThenInstanceGroupEventWillBeCalledWith() {
        when(details.isPurgeZombies()).thenReturn(Boolean.TRUE);
        Stack stack = TestUtil.stack();
        stack.setId(STACK_ID);
        StackDto stackDto = spy(StackDto.class);
        InstanceGroup instanceGroup = stack.getInstanceGroups().iterator().next();
        instanceGroup.setGroupName(HOST_GROUP_NAME);
        instanceGroup.getInstanceMetaData().iterator().next().setInstanceStatus(InstanceStatus.ZOMBIE);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackDto.getZombieInstanceMetaData()).thenReturn(new ArrayList<>(instanceGroup.getInstanceMetaData()));

        underTest.clusterDownscaleStarted(STACK_ID, new ClusterDownscaleTriggerEvent(null, STACK_ID, Set.of(HOST_GROUP_NAME), null, details));

        verify(flowMessageService, times(1)).fireEventAndLog(STACK_ID,
                Status.UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_REMOVING_ZOMBIE_NODES, "1");
        verify(flowMessageService, times(1)).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(),
                ResourceEvent.CLUSTER_SCALING_DOWN_ZOMBIE_NODES, "worker");
        verify(clusterService, times(1)).updateClusterStatusByStackId(STACK_ID, DetailedStackStatus.DOWNSCALE_IN_PROGRESS);
        verify(stackDtoService, times(1)).getById(STACK_ID);
        verify(flowMessageService, never()).fireInstanceGroupEventAndLog(eq(STACK_ID), anyString(), anyString(), any(ResourceEvent.class), anyString());
    }
}
