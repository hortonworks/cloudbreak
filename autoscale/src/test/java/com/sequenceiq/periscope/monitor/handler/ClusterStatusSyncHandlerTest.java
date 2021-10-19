package com.sequenceiq.periscope.monitor.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.monitor.event.ClusterStatusSyncEvent;
import com.sequenceiq.periscope.service.AltusMachineUserService;
import com.sequenceiq.periscope.service.ClusterService;

public class ClusterStatusSyncHandlerTest {

    private static final long AUTOSCALE_CLUSTER_ID = 1L;

    private static final String CLOUDBREAK_STACK_CRN = "someCrn";

    @Mock
    private ClusterService clusterService;

    @Mock
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Mock
    private AltusMachineUserService altusMachineUserService;

    @InjectMocks
    private ClusterStatusSyncHandler underTest;

    private String testEnvironmentCrn = "testEnvironmentCrn";

    private String testTenant = "testTenant";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testOnApplicationEventWhenCBStatusDeleted() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getStackStatusByCrn(anyString())).thenReturn(getStackResponse(Status.DELETE_COMPLETED));
        when(clusterService.countByEnvironmentCrn("testEnvironmentCrn")).thenReturn(2);

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).removeById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).save(cluster);
        verify(cloudbreakCommunicator).getStackStatusByCrn(CLOUDBREAK_STACK_CRN);
        verify(altusMachineUserService, never()).deleteMachineUserForEnvironment(anyString(), anyString(), anyString());
    }

    @Test
    public void testOnApplicationEventWhenCBStatusDeletedAndNoMoreEnvironmentClusters() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        cluster.setMachineUserCrn("testMachineUserCrn");
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(clusterService.countByEnvironmentCrn("testEnvironmentCrn")).thenReturn(1);
        when(cloudbreakCommunicator.getStackStatusByCrn(anyString())).thenReturn(getStackResponse(Status.DELETE_COMPLETED));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).removeById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).save(cluster);
        verify(cloudbreakCommunicator).getStackStatusByCrn(CLOUDBREAK_STACK_CRN);
        verify(altusMachineUserService, times(1))
                .deleteMachineUserForEnvironment("testTenant", cluster.getMachineUserCrn(), "testEnvironmentCrn");
    }

    @Test
    public void testOnApplicationEventWhenCBStatusStoppedAndPeriscopeClusterRunning() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getStackStatusByCrn(anyString())).thenReturn(getStackResponse(Status.STOPPED));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(cloudbreakCommunicator).getStackStatusByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testOnApplicationEventWhenCBStatusStoppedAndPeriscopeClusterSuspended() {
        Cluster cluster = getACluster(ClusterState.SUSPENDED);
        cluster.setState(ClusterState.SUSPENDED);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getStackStatusByCrn(anyString())).thenReturn(getStackResponse(Status.STOPPED));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, never()).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(cloudbreakCommunicator).getStackStatusByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testOnApplicationEventWhenCBStackStatusActiveCBClusterStatusInactive() {
        Cluster cluster = getACluster(ClusterState.SUSPENDED);
        when(clusterService.findById(anyLong())).thenReturn(cluster);

        StackStatusV4Response stackStatusV4Response = new StackStatusV4Response();
        stackStatusV4Response.setStatus(Status.AVAILABLE);
        stackStatusV4Response.setClusterStatus(Status.UPDATE_IN_PROGRESS);
        when(cloudbreakCommunicator.getStackStatusByCrn(anyString())).thenReturn(stackStatusV4Response);

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, never()).setState(anyLong(), any(ClusterState.class));
        verify(cloudbreakCommunicator).getStackStatusByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testOnApplicationEventWhenCBStackStatusInActiveCBClusterStatusActive() {
        Cluster cluster = getACluster(ClusterState.SUSPENDED);
        when(clusterService.findById(anyLong())).thenReturn(cluster);

        StackStatusV4Response stackStatusV4Response = new StackStatusV4Response();
        stackStatusV4Response.setStatus(Status.UPDATE_IN_PROGRESS);
        stackStatusV4Response.setClusterStatus(Status.AVAILABLE);
        when(cloudbreakCommunicator.getStackStatusByCrn(anyString())).thenReturn(stackStatusV4Response);

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, never()).setState(anyLong(), any(ClusterState.class));
        verify(cloudbreakCommunicator).getStackStatusByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testOnApplicationEventWhenCBStackStatusActiveCBClusterStatusActive() {
        Cluster cluster = getACluster(ClusterState.SUSPENDED);
        when(clusterService.findById(anyLong())).thenReturn(cluster);


        AutoscaleStackV4Response mockAutoscaleResponse = mock(AutoscaleStackV4Response.class);
        StackStatusV4Response stackStatusV4Response = new StackStatusV4Response();
        stackStatusV4Response.setStatus(Status.AVAILABLE);
        stackStatusV4Response.setClusterStatus(Status.AVAILABLE);
        when(cloudbreakCommunicator.getStackStatusByCrn(anyString())).thenReturn(stackStatusV4Response);
        when(cloudbreakCommunicator.getAutoscaleClusterByCrn(CLOUDBREAK_STACK_CRN)).thenReturn(mockAutoscaleResponse);
        when(mockAutoscaleResponse.getEnvironmentCrn()).thenReturn("environmentcrn");

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.RUNNING);
        verify(cloudbreakCommunicator).getStackStatusByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testOnApplicationEventWhenCBStatusRunningAndPeriscopeClusterRunning() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getStackStatusByCrn(anyString())).thenReturn(getStackResponse(Status.AVAILABLE));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, never()).setState(anyLong(), any(ClusterState.class));
        verify(cloudbreakCommunicator).getStackStatusByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testOnApplicationEventWhenCBStatusRunningAndPeriscopeClusterStopped() {
        Cluster cluster = getACluster(ClusterState.SUSPENDED);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getStackStatusByCrn(anyString())).thenReturn(getStackResponse(Status.AVAILABLE));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.RUNNING);
        verify(cloudbreakCommunicator).getStackStatusByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testOnApplicationEventWhenCBStatusUnreachable() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getStackStatusByCrn(anyString())).thenReturn(getStackResponse(Status.UNREACHABLE));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(cloudbreakCommunicator).getStackStatusByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testOnApplicationEventWhenCBStatusNodeFailure() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getStackStatusByCrn(anyString())).thenReturn(getStackResponse(Status.NODE_FAILURE));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(cloudbreakCommunicator).getStackStatusByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test(expected = RuntimeException.class)
    public void testOnApplicationEventWhenCBStatusFails() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getStackStatusByCrn(anyString())).thenThrow(new RuntimeException("some error in communication"));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator).getStackStatusByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testOnApplicationEventWhenCBStatusNotAvailable() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getStackStatusByCrn(anyString())).thenReturn(getStackResponse(null));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator).getStackStatusByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testOnApplicationEventWhenStopStartScalingEnabledAndClusterIsScaledDown() {
        // At the moment, there's no good way to determine if the cluster is scaled down,
        //  so this test essentially exercises existing flow with the stopStartMechanism enabled.
        Cluster cluster = getACluster(ClusterState.RUNNING);
        cluster.setStopStartScalingEnabled(Boolean.TRUE);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getStackStatusByCrn(anyString())).thenReturn(getStackResponse(Status.AVAILABLE));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        assertEquals(ClusterState.RUNNING, cluster.getState());
        verify(clusterService).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).setState(anyLong(), any(ClusterState.class));
        verify(cloudbreakCommunicator).getStackStatusByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testOnApplicationEventWhenStopStartScalingEnabledAndClusterIsScaledUp() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        cluster.setStopStartScalingEnabled(Boolean.TRUE);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getStackStatusByCrn(anyString())).thenReturn(getStackResponse(Status.AVAILABLE));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).setState(anyLong(), any(ClusterState.class));
        verify(cloudbreakCommunicator).getStackStatusByCrn(CLOUDBREAK_STACK_CRN);
    }

    private StackStatusV4Response getStackResponse(Status clusterStatus) {
        StackStatusV4Response stackResponse = new StackStatusV4Response();
        stackResponse.setStatus(clusterStatus);
        stackResponse.setClusterStatus(clusterStatus);
        return stackResponse;
    }

    private Cluster getACluster(ClusterState clusterState) {
        Cluster cluster = new Cluster();
        cluster.setId(AUTOSCALE_CLUSTER_ID);
        cluster.setStackCrn(CLOUDBREAK_STACK_CRN);
        cluster.setState(clusterState);
        cluster.setEnvironmentCrn(testEnvironmentCrn);
        cluster.setMachineUserCrn("testMachineUser");
        cluster.setStopStartScalingEnabled(Boolean.FALSE);

        ClusterPertain clusterPertain = new ClusterPertain();
        clusterPertain.setTenant(testTenant);
        cluster.setClusterPertain(clusterPertain);
        return cluster;
    }
}