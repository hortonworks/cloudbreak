package com.sequenceiq.periscope.monitor.handler;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.monitor.event.ClusterDeleteEvent;
import com.sequenceiq.periscope.service.ClusterService;

@ExtendWith(MockitoExtension.class)
class ClusterDeleteHandlerTest {

    private static final Long AUTOSCALE_CLUSTER_ID = 1L;

    private static final String CLOUDBREAK_STACK_CRN = "crn:cdp:datahub:us-west-1:tenant:datahub:012e39ab-e972-4345-87ee-0e12b47aa5b8";

    @Mock
    private ClusterService clusterService;

    @Mock
    private CloudbreakCommunicator cloudbreakCommunicator;

    @InjectMocks
    private ClusterDeleteHandler underTest;

    @Test
    void testOnApplicationEventReturnsIfClusterIsNull() {
        when(clusterService.findById(anyLong())).thenReturn(null);

        underTest.onApplicationEvent(new ClusterDeleteEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).findById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator, never()).getStackStatusByCrn(anyString());
    }

    @Test
    void testOnApplicationEventWhenCBClusterDeleted() {
        Cluster cluster = getCluster(ClusterState.RUNNING);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getStackStatusByCrn(anyString())).thenReturn(getStackResponse(Status.DELETE_COMPLETED));

        underTest.onApplicationEvent(new ClusterDeleteEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator).getStackStatusByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    void testOnApplicationEventWhenCBClusterRunning() {
        Cluster cluster = getCluster(ClusterState.RUNNING);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getStackStatusByCrn(anyString())).thenReturn(getStackResponse(Status.AVAILABLE));

        underTest.onApplicationEvent(new ClusterDeleteEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator).getStackStatusByCrn(CLOUDBREAK_STACK_CRN);
    }

    private StackStatusV4Response getStackResponse(Status clusterStatus) {
        StackStatusV4Response stackResponse = new StackStatusV4Response();
        stackResponse.setStatus(clusterStatus);
        stackResponse.setClusterStatus(clusterStatus);
        return stackResponse;
    }

    private Cluster getCluster(ClusterState clusterState) {
        Cluster cluster = new Cluster();
        cluster.setId(AUTOSCALE_CLUSTER_ID);
        cluster.setStackCrn(CLOUDBREAK_STACK_CRN);
        cluster.setState(clusterState);
        cluster.setStopStartScalingEnabled(Boolean.FALSE);

        ClusterPertain clusterPertain = new ClusterPertain();
        cluster.setClusterPertain(clusterPertain);
        return cluster;
    }
}