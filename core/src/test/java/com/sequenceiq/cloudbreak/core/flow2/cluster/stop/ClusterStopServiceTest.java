package com.sequenceiq.cloudbreak.core.flow2.cluster.stop;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_STOPPED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_STOP_FAILED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
class ClusterStopServiceTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private ClusterService clusterService;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private StackUtil stackUtil;

    @InjectMocks
    private ClusterStopService underTest;

    @Test
    public void testStoppingCluster() {
        Cluster cluster = TestUtil.cluster();
        when(clusterService.retrieveClusterByStackIdWithoutAuth(any())).thenReturn(Optional.of(cluster));

        underTest.stoppingCluster(STACK_ID);

        verify(clusterService, times(1)).updateCluster(eq(cluster));
        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_IN_PROGRESS.name()), eq(ResourceEvent.CLUSTER_STOPPING));
        verify(clusterService, times(1)).updateClusterStatusByStackId(eq(STACK_ID), eq(DetailedStackStatus.STOP_IN_PROGRESS));
    }

    @Test
    public void testClusterStopFinished() {
        underTest.clusterStopFinished(STACK_ID);

        verify(clusterService, times(1)).updateClusterStatusByStackId(eq(STACK_ID), eq(DetailedStackStatus.CLUSTER_STOPPED));
        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID), eq(Status.STOPPED.name()), eq(CLUSTER_STOPPED));
    }

    @Test
    public void testHandleClusterStopFailureAndContinue() {
        underTest.handleClusterStopFailureAndContinue(STACK_ID, "reason");

        verify(clusterService, times(1)).updateClusterStatusByStackId(eq(STACK_ID), eq(DetailedStackStatus.CLUSTER_STOP_FAILED), eq("reason"));
        verify(flowMessageService, times(1)).fireEventAndLog(eq(STACK_ID), eq(Status.STOP_FAILED.name()), eq(CLUSTER_STOP_FAILED), eq("reason"));
    }
}