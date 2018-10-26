package com.sequenceiq.periscope.monitor.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetaDataJson;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.StackResponseUtils;

public class UpdateFailedHandlerTest {
    private static final long AUTOSCALE_CLUSTER_ID = 1L;

    private static final long CLOUDBREAK_STACK_ID = 2L;

    @Mock
    private ClusterService clusterService;

    @Mock
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Mock
    private StackResponseUtils stackResponseUtils;

    @InjectMocks
    private UpdateFailedHandler underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testOnApplicationEventWhenStatusDelete() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getById(anyLong())).thenReturn(getStackResponse(Status.DELETE_IN_PROGRESS, Status.DELETE_IN_PROGRESS));

        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).removeById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).save(cluster);
        verify(cloudbreakCommunicator).getById(CLOUDBREAK_STACK_ID);
    }

    @Test
    public void testOnApplicationEventWhenGetStatusFails() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getById(anyLong())).thenThrow(new RuntimeException("some error in communication"));

        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).setState(cluster, ClusterState.SUSPENDED);
        verify(clusterService).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator).getById(CLOUDBREAK_STACK_ID);
    }

    @Test
    public void testOnApplicationEventWhenFailsFirstTime() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getById(anyLong())).thenReturn(getStackResponse(Status.AVAILABLE, Status.AVAILABLE));
        when(stackResponseUtils.getNotTerminatedPrimaryGateways(any())).thenReturn(getPrimaryGateway());

        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).setState(cluster, ClusterState.SUSPENDED);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator, never()).failureReport(anyLong(), any());
    }

    @Test
    public void testOnApplicationEventWhenFailsFourTimes() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getById(anyLong())).thenReturn(getStackResponse(Status.AVAILABLE, Status.AVAILABLE));
        when(stackResponseUtils.getNotTerminatedPrimaryGateways(any())).thenReturn(getPrimaryGateway());

        IntStream.range(0, 3).forEach(i -> underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID)));
        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, times(4)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).setState(cluster, ClusterState.SUSPENDED);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator, times(4)).getById(CLOUDBREAK_STACK_ID);
        verify(cloudbreakCommunicator, never()).failureReport(anyLong(), any());
    }

    @Test
    public void testOnApplicationEventWhenFailsFiveTimes() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getById(anyLong())).thenReturn(getStackResponse(Status.AVAILABLE, Status.AVAILABLE));
        when(stackResponseUtils.getNotTerminatedPrimaryGateways(any())).thenReturn(getPrimaryGateway());

        IntStream.range(0, 4).forEach(i -> underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID)));
        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, times(5)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).setState(cluster, ClusterState.SUSPENDED);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator, times(5)).getById(CLOUDBREAK_STACK_ID);
        verify(cloudbreakCommunicator).failureReport(eq(CLOUDBREAK_STACK_ID), any());
    }

    @Test
    public void testOnApplicationEventWhenFailsFiveTimesNotAvailable() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getById(anyLong())).thenReturn(getStackResponse(Status.AVAILABLE, Status.UPDATE_FAILED));
        when(stackResponseUtils.getNotTerminatedPrimaryGateways(any())).thenReturn(getPrimaryGateway());

        IntStream.range(0, 4).forEach(i -> underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID)));
        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, times(5)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).setState(cluster, ClusterState.SUSPENDED);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator, times(5)).getById(CLOUDBREAK_STACK_ID);
        verify(cloudbreakCommunicator, never()).failureReport(eq(CLOUDBREAK_STACK_ID), any());
    }

    @Test
    public void testOnApplicationEventWhenReportFailureThrows() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getById(anyLong())).thenReturn(getStackResponse(Status.AVAILABLE, Status.AVAILABLE));
        when(stackResponseUtils.getNotTerminatedPrimaryGateways(any())).thenReturn(getPrimaryGateway());
        doThrow(new RuntimeException("error sending failure report")).when(cloudbreakCommunicator).failureReport(anyLong(), any());

        IntStream.range(0, 4).forEach(i -> underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID)));
        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, times(5)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).setState(cluster, ClusterState.SUSPENDED);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator).failureReport(eq(CLOUDBREAK_STACK_ID), any());
    }

    @Test
    public void testOnApplicationEventWhenFailsFourTimesWithStatusNull() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getById(anyLong())).thenReturn(getStackResponse(null, null));
        when(stackResponseUtils.getNotTerminatedPrimaryGateways(any())).thenReturn(getPrimaryGateway());

        IntStream.range(0, 3).forEach(i -> underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID)));
        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, times(4)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).setState(cluster, ClusterState.SUSPENDED);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator, times(4)).getById(CLOUDBREAK_STACK_ID);
        verify(cloudbreakCommunicator, never()).failureReport(anyLong(), any());
    }

    @Test
    public void testOnApplicationEventWhenFailsFiveTimesWithStatusNull() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getById(anyLong())).thenReturn(getStackResponse(null, null));
        when(stackResponseUtils.getNotTerminatedPrimaryGateways(any())).thenReturn(getPrimaryGateway());

        IntStream.range(0, 4).forEach(i -> underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID)));
        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, times(5)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).setState(cluster, ClusterState.SUSPENDED);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator, times(5)).getById(CLOUDBREAK_STACK_ID);
        verify(cloudbreakCommunicator, never()).failureReport(anyLong(), any());
    }

    @Test
    public void testOnApplicationEventWhenFailsAfterClusterRemove() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(null);
        when(cloudbreakCommunicator.getById(anyLong())).thenReturn(getStackResponse(null, null));
        when(stackResponseUtils.getNotTerminatedPrimaryGateways(any())).thenReturn(getPrimaryGateway());

        IntStream.range(0, 4).forEach(i -> underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID)));
        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, times(5)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).setState(cluster, ClusterState.SUSPENDED);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator, never()).getById(CLOUDBREAK_STACK_ID);
        verify(cloudbreakCommunicator, never()).failureReport(anyLong(), any());
    }

    private Optional<InstanceMetaDataJson> getPrimaryGateway() {
        InstanceMetaDataJson instanceMetaDataJson = new InstanceMetaDataJson();
        instanceMetaDataJson.setDiscoveryFQDN("");
        return Optional.of(instanceMetaDataJson);
    }

    private StackResponse getStackResponse(Status stackStatus, Status clusterStatus) {
        StackResponse stackResponse = new StackResponse();
        stackResponse.setStatus(stackStatus);
        ClusterResponse clusterResponse = new ClusterResponse();
        clusterResponse.setStatus(clusterStatus);
        stackResponse.setCluster(clusterResponse);
        return stackResponse;
    }

    private Cluster getARunningCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(AUTOSCALE_CLUSTER_ID);
        cluster.setStackId(CLOUDBREAK_STACK_ID);
        cluster.setState(ClusterState.RUNNING);
        return cluster;
    }
}
