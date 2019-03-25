package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public class ClusterDownscaleServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String HOST_GROUP_NAME = "worker";

    private static final Set<Long> PRIVATE_IDS = Set.of(1L, 2L);

    @Mock
    private StackService stackService;

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

    @InjectMocks
    private ClusterDownscaleService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testClusterDownscaleStartedWhenScalingAdjustmentIsGivenAndItIsPositiveThenInstanceGroupEventWillBeCalledThisNumber() {
        doNothing().when(flowMessageService).fireEventAndLog(STACK_ID, Msg.CLUSTER_SCALING_DOWN, Status.UPDATE_IN_PROGRESS.name());

        underTest.clusterDownscaleStarted(STACK_ID, HOST_GROUP_NAME, 1, PRIVATE_IDS, details);

        verify(flowMessageService, times(1)).fireInstanceGroupEventAndLog(STACK_ID, Msg.CLUSTER_REMOVING_NODE_FROM_HOSTGROUP,
                Status.UPDATE_IN_PROGRESS.name(), HOST_GROUP_NAME, 1, HOST_GROUP_NAME);
        verify(flowMessageService, times(1)).fireEventAndLog(STACK_ID, Msg.CLUSTER_SCALING_DOWN, Status.UPDATE_IN_PROGRESS.name());
        verify(clusterService, times(1)).updateClusterStatusByStackId(STACK_ID, Status.UPDATE_IN_PROGRESS);
        verify(stackService, times(0)).getByIdWithListsInTransaction(anyLong());
        verify(stackService, times(0)).getByIdWithListsInTransaction(STACK_ID);
        verify(stackService, times(0)).getHostNamesForPrivateIds(anyListOf(InstanceMetaData.class), anySet());
        verify(flowMessageService, times(0)).fireInstanceGroupEventAndLog(eq(STACK_ID), any(Msg.class), anyString(), anyString(),
                anyListOf(String.class), anyString());
    }

    @Test
    public void testClusterDownscaleStartedWhenScalingAdjustmentIsGivenAndItIsNegativeThenInstanceGroupEventWillBeCalledWithTheAbsoluteValueOfThisNumber() {
        doNothing().when(flowMessageService).fireEventAndLog(STACK_ID, Msg.CLUSTER_SCALING_DOWN, Status.UPDATE_IN_PROGRESS.name());

        underTest.clusterDownscaleStarted(STACK_ID, HOST_GROUP_NAME, -1, PRIVATE_IDS, details);

        verify(flowMessageService, times(1)).fireInstanceGroupEventAndLog(STACK_ID, Msg.CLUSTER_REMOVING_NODE_FROM_HOSTGROUP,
                Status.UPDATE_IN_PROGRESS.name(), HOST_GROUP_NAME, 1, HOST_GROUP_NAME);
        verify(flowMessageService, times(1)).fireEventAndLog(STACK_ID, Msg.CLUSTER_SCALING_DOWN, Status.UPDATE_IN_PROGRESS.name());
        verify(clusterService, times(1)).updateClusterStatusByStackId(STACK_ID, Status.UPDATE_IN_PROGRESS);
        verify(stackService, times(0)).getByIdWithListsInTransaction(anyLong());
        verify(stackService, times(0)).getByIdWithListsInTransaction(STACK_ID);
        verify(stackService, times(0)).getHostNamesForPrivateIds(anyListOf(InstanceMetaData.class), anySet());
        verify(flowMessageService, times(0)).fireInstanceGroupEventAndLog(eq(STACK_ID), any(Msg.class), anyString(), anyString(),
                anyListOf(String.class), anyString());
    }
}