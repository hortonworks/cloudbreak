package com.sequenceiq.periscope.monitor.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.FailedNode;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.repository.FailedNodeRepository;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.StackResponseUtils;

public class UpdateFailedHandlerTest {

    private static final long AUTOSCALE_CLUSTER_ID = 1L;

    private static final long CLOUDBREAK_STACK_ID = 2L;

    private static final String CLOUDBREAK_STACK_CRN = "someCrn";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private ClusterService clusterService;

    @Mock
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Mock
    private StackResponseUtils stackResponseUtils;

    @Mock
    private FailedNodeRepository failedNodeRepository;

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
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getStackResponse(Status.DELETE_COMPLETED, Status.DELETE_COMPLETED));

        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).removeById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).save(cluster);
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testOnApplicationEventWhenGetStatusFails() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenThrow(new RuntimeException("some error in communication"));

        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).setState(cluster, ClusterState.SUSPENDED);
        verify(clusterService).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testOnApplicationEventWhenFailsFirstTime() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getStackResponse(Status.AVAILABLE, Status.AVAILABLE));
        when(stackResponseUtils.getNotTerminatedPrimaryGateways(any())).thenReturn(getPrimaryGateway());

        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).setState(cluster, ClusterState.SUSPENDED);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
    }

    @Test
    public void testOnApplicationEventWhenFailsFourTimes() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getStackResponse(Status.AVAILABLE, Status.AVAILABLE));
        when(stackResponseUtils.getNotTerminatedPrimaryGateways(any())).thenReturn(getPrimaryGateway());

        IntStream.range(0, 3).forEach(i -> underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID)));
        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, times(4)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).setState(cluster, ClusterState.SUSPENDED);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator, times(4)).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testOnApplicationEventWhenFailsFiveTimes() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getStackResponse(Status.AVAILABLE, Status.AVAILABLE));
        when(stackResponseUtils.getNotTerminatedPrimaryGateways(any())).thenReturn(getPrimaryGateway());

        IntStream.range(0, 4).forEach(i -> underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID)));
        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, times(5)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).setState(cluster, ClusterState.SUSPENDED);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator, times(5)).getByCrn(CLOUDBREAK_STACK_CRN);
        FailedNode failedNode = new FailedNode();
        failedNode.setName("master");
        failedNode.setClusterId(AUTOSCALE_CLUSTER_ID);
        verify(failedNodeRepository).save(failedNode);
    }

    @Test
    public void testOnApplicationEventWhenFailsFiveTimesNotAvailable() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getStackResponse(Status.AVAILABLE, Status.UPDATE_FAILED));
        when(stackResponseUtils.getNotTerminatedPrimaryGateways(any())).thenReturn(getPrimaryGateway());

        IntStream.range(0, 4).forEach(i -> underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID)));
        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, times(5)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).setState(cluster, ClusterState.SUSPENDED);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator, times(5)).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testOnApplicationEventWhenReportFailureThrows() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getStackResponse(Status.AVAILABLE, Status.AVAILABLE));
        when(stackResponseUtils.getNotTerminatedPrimaryGateways(any())).thenReturn(getPrimaryGateway());
        doThrow(new RuntimeException("error saving failednodes")).when(failedNodeRepository).save(any());


        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("error saving failednodes");

        IntStream.range(0, 4).forEach(i -> underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID)));
        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, times(5)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).setState(cluster, ClusterState.SUSPENDED);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        FailedNode failedNode = new FailedNode();
        failedNode.setName("master");
        failedNode.setClusterId(AUTOSCALE_CLUSTER_ID);
        verify(failedNodeRepository).save(failedNode);
    }

    @Test
    public void testOnApplicationEventWhenFailsFourTimesWithStatusNull() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getStackResponse(null, null));
        when(stackResponseUtils.getNotTerminatedPrimaryGateways(any())).thenReturn(getPrimaryGateway());

        IntStream.range(0, 3).forEach(i -> underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID)));
        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, times(4)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).setState(cluster, ClusterState.SUSPENDED);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator, times(4)).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testOnApplicationEventWhenFailsFiveTimesWithStatusNull() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getStackResponse(null, null));
        when(stackResponseUtils.getNotTerminatedPrimaryGateways(any())).thenReturn(getPrimaryGateway());

        IntStream.range(0, 4).forEach(i -> underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID)));
        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, times(5)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).setState(cluster, ClusterState.SUSPENDED);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator, times(5)).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testOnApplicationEventWhenFailsAfterClusterRemove() {
        Cluster cluster = getARunningCluster();
        when(clusterService.findById(anyLong())).thenReturn(null);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getStackResponse(null, null));
        when(stackResponseUtils.getNotTerminatedPrimaryGateways(any())).thenReturn(getPrimaryGateway());

        IntStream.range(0, 4).forEach(i -> underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID)));
        underTest.onApplicationEvent(new UpdateFailedEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, times(5)).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).setState(cluster, ClusterState.SUSPENDED);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator, never()).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    private Optional<InstanceMetaDataV4Response> getPrimaryGateway() {
        InstanceMetaDataV4Response instanceMetaDataJson = new InstanceMetaDataV4Response();
        instanceMetaDataJson.setDiscoveryFQDN("master");
        return Optional.of(instanceMetaDataJson);
    }

    private StackV4Response getStackResponse(Status stackStatus, Status clusterStatus) {
        StackV4Response stackResponse = new StackV4Response();
        stackResponse.setStatus(stackStatus);
        ClusterV4Response clusterResponse = new ClusterV4Response();
        clusterResponse.setStatus(clusterStatus);
        stackResponse.setCluster(clusterResponse);
        return stackResponse;
    }

    private Cluster getARunningCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(AUTOSCALE_CLUSTER_ID);
        cluster.setStackCrn(CLOUDBREAK_STACK_CRN);
        cluster.setState(ClusterState.RUNNING);
        return cluster;
    }
}
