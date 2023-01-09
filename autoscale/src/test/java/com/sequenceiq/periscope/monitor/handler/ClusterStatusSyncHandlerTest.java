package com.sequenceiq.periscope.monitor.handler;

import static com.sequenceiq.periscope.utils.MockStackResponseGenerator.getBasicMockStackResponse;
import static com.sequenceiq.periscope.utils.MockStackResponseGenerator.getMockStackResponseWithDependentHostGroup;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.DependentHostGroupsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.LoadAlertConfiguration;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.monitor.event.ClusterStatusSyncEvent;
import com.sequenceiq.periscope.service.AltusMachineUserService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.DependentHostGroupsService;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@ExtendWith(MockitoExtension.class)
class ClusterStatusSyncHandlerTest {

    private static final long AUTOSCALE_CLUSTER_ID = 1L;

    private static final String CLOUDBREAK_STACK_CRN = "someCrn";

    private static final String TEST_ENVIRONMENT_CRN = "testEnvironmentCrn";

    private static final String TEST_TENANT = "testTenant";

    @Mock
    private ClusterService clusterService;

    @Mock
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Mock
    private AltusMachineUserService altusMachineUserService;

    @Mock
    private DependentHostGroupsService dependentHostGroupsService;

    @Mock
    private ClouderaManagerCommunicator cmCommunicator;

    @Mock
    private StackResponseUtils stackResponseUtils;

    @InjectMocks
    private ClusterStatusSyncHandler underTest;

    @Test
    void testOnApplicationEventWhenCBStatusDeleted() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getBasicMockStackResponse(Status.DELETE_COMPLETED));
        when(clusterService.countByEnvironmentCrn("testEnvironmentCrn")).thenReturn(2);

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).removeById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).save(cluster);
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
        verify(altusMachineUserService, never()).deleteMachineUserForEnvironment(anyString(), anyString(), anyString());
    }

    @Test
    void testOnApplicationEventWhenCBStatusDeletedAndNoMoreEnvironmentClusters() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        cluster.setMachineUserCrn("testMachineUserCrn");
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(clusterService.countByEnvironmentCrn("testEnvironmentCrn")).thenReturn(1);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getBasicMockStackResponse(Status.DELETE_COMPLETED));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).removeById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).save(cluster);
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
        verify(altusMachineUserService, times(1))
                .deleteMachineUserForEnvironment("testTenant", cluster.getMachineUserCrn(), "testEnvironmentCrn");
    }

    @Test
    void testOnApplicationEventWhenCBStatusStoppedAndPeriscopeClusterRunning() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getBasicMockStackResponse(Status.STOPPED));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    void testOnApplicationEventWhenCBStatusStoppedAndPeriscopeClusterSuspended() {
        Cluster cluster = getACluster(ClusterState.SUSPENDED);
        cluster.setState(ClusterState.SUSPENDED);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getBasicMockStackResponse(Status.STOPPED));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, never()).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    void testOnApplicationEventWhenCBStatusRunningAndPeriscopeClusterRunning() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(stackResponseUtils.primaryGatewayHealthy(any(StackV4Response.class))).thenReturn(true);
        when(cmCommunicator.isClusterManagerRunning(any(Cluster.class))).thenReturn(true);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getBasicMockStackResponse(Status.AVAILABLE));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, never()).setState(anyLong(), any(ClusterState.class));
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    void testOnApplicationEventWhenCBStatusRunningAndPeriscopeClusterStopped() {
        Cluster cluster = getACluster(ClusterState.SUSPENDED);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getBasicMockStackResponse(Status.AVAILABLE));
        when(stackResponseUtils.primaryGatewayHealthy(any(StackV4Response.class))).thenReturn(true);
        when(cmCommunicator.isClusterManagerRunning(any(Cluster.class))).thenReturn(true);

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.RUNNING);
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    void testOnApplicationEventWhenCBStatusUnreachable() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getBasicMockStackResponse(Status.UNREACHABLE));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    void testOnApplicationEventWhenCBStatusNodeFailure() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getBasicMockStackResponse(Status.NODE_FAILURE));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    void testOnApplicationEventWhenCBStatusFails() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenThrow(new RuntimeException("some error in communication"));

        assertThrows(RuntimeException.class, () -> underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID)));

        verify(clusterService).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    void testOnApplicationEventWhenCBStatusNotAvailable() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getBasicMockStackResponse(null));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, never()).removeById(AUTOSCALE_CLUSTER_ID);
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    void testOnApplicationEventWhenStopStartScalingEnabledAndClusterIsScaledDown() {
        // At the moment, there's no good way to determine if the cluster is scaled down,
        //  so this test essentially exercises existing flow with the stopStartMechanism enabled.
        Cluster cluster = getACluster(ClusterState.RUNNING);
        cluster.setStopStartScalingEnabled(Boolean.TRUE);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getBasicMockStackResponse(Status.AVAILABLE));
        when(dependentHostGroupsService.getDependentHostGroupsForPolicyHostGroups(anyString(), anySet()))
                .thenReturn(getUndefinedDependentHostGroupResponse("compute"));
        when(cmCommunicator.isClusterManagerRunning(any(Cluster.class))).thenReturn(true);
        when(stackResponseUtils.primaryGatewayHealthy(any(StackV4Response.class))).thenReturn(true);

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        assertEquals(ClusterState.RUNNING, cluster.getState());
        verify(clusterService).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).setState(anyLong(), any(ClusterState.class));
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    void testOnApplicationEventWhenStopStartScalingEnabledAndClusterIsScaledUp() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        cluster.setStopStartScalingEnabled(Boolean.TRUE);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getBasicMockStackResponse(Status.AVAILABLE));
        when(dependentHostGroupsService.getDependentHostGroupsForPolicyHostGroups(anyString(), anySet()))
                .thenReturn(getUndefinedDependentHostGroupResponse("compute"));
        when(cmCommunicator.isClusterManagerRunning(any(Cluster.class))).thenReturn(true);
        when(stackResponseUtils.primaryGatewayHealthy(any(StackV4Response.class))).thenReturn(true);

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).findById(AUTOSCALE_CLUSTER_ID);
        verify(clusterService, never()).setState(anyLong(), any(ClusterState.class));
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    void testOnApplicationEventWhenStopStartScalingEnabledAndDependentHostUnhealthyAndCmHealthy() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        cluster.setStopStartScalingEnabled(Boolean.TRUE);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getMockStackResponseWithDependentHostGroup(Status.AVAILABLE,
                Set.of("gateway1"), InstanceStatus.SERVICES_UNHEALTHY));
        when(stackResponseUtils.getUnhealthyDependentHosts(any(StackV4Response.class), any(DependentHostGroupsV4Response.class),
                anyString())).thenCallRealMethod();
        when(dependentHostGroupsService.getDependentHostGroupsForPolicyHostGroups(anyString(), anySet()))
                .thenReturn(getDependentHostGroupsResponse("compute", "master", "gateway1"));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    void testOnApplicationEventWhenStopStartScalingEnabledAndMultipleDependentHostsUnhealthyAndCmHealthy() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        cluster.setStopStartScalingEnabled(Boolean.TRUE);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getMockStackResponseWithDependentHostGroup(Status.AVAILABLE,
                Set.of("master", "gateway1"), InstanceStatus.SERVICES_UNHEALTHY));
        when(stackResponseUtils.getUnhealthyDependentHosts(any(StackV4Response.class), any(DependentHostGroupsV4Response.class),
                anyString())).thenCallRealMethod();
        when(dependentHostGroupsService.getDependentHostGroupsForPolicyHostGroups(anyString(), anySet()))
                .thenReturn(getDependentHostGroupsResponse("compute", "master", "gateway1"));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    void testOnApplicationEventWhenStopStartScalingEnabledAndDependentHostsHealthyAndCmHealthy() {
        Cluster cluster = getACluster(ClusterState.SUSPENDED);
        cluster.setStopStartScalingEnabled(Boolean.TRUE);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getMockStackResponseWithDependentHostGroup(Status.AVAILABLE, Set.of("master", "gateway1"),
                InstanceStatus.SERVICES_HEALTHY));
        when(stackResponseUtils.getUnhealthyDependentHosts(any(StackV4Response.class), any(DependentHostGroupsV4Response.class),
                anyString())).thenCallRealMethod();
        when(dependentHostGroupsService.getDependentHostGroupsForPolicyHostGroups(anyString(), anySet()))
                .thenReturn(getDependentHostGroupsResponse("compute", "master", "gateway1"));
        when(cmCommunicator.isClusterManagerRunning(any(Cluster.class))).thenReturn(true);
        when(stackResponseUtils.primaryGatewayHealthy(any(StackV4Response.class))).thenReturn(true);

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.RUNNING);
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    void testOnApplicationEventWhenStopStartScalingEnabledAndStackInNodeFailureButDependentHostsAndCmHealthy() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        cluster.setStopStartScalingEnabled(Boolean.TRUE);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getMockStackResponseWithDependentHostGroup(Status.NODE_FAILURE,
                Set.of("master", "gateway1"), InstanceStatus.SERVICES_HEALTHY));
        when(stackResponseUtils.getUnhealthyDependentHosts(any(StackV4Response.class), any(DependentHostGroupsV4Response.class),
                anyString())).thenCallRealMethod();
        when(dependentHostGroupsService.getDependentHostGroupsForPolicyHostGroups(anyString(), anySet()))
                .thenReturn(getDependentHostGroupsResponse("compute", "master", "gateway1"));
        when(cmCommunicator.isClusterManagerRunning(any(Cluster.class))).thenReturn(true);
        when(stackResponseUtils.primaryGatewayHealthy(any(StackV4Response.class))).thenReturn(true);

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService, never()).setState(anyLong(), any(ClusterState.class));
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    void testOnApplicationEventWhenStopStartScalingEnabledAndDependentHostsHealthyButCmUnhealthy() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        cluster.setStopStartScalingEnabled(Boolean.TRUE);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getMockStackResponseWithDependentHostGroup(Status.AVAILABLE, Set.of("master", "gateway1"),
                InstanceStatus.SERVICES_HEALTHY));
        when(stackResponseUtils.getUnhealthyDependentHosts(any(StackV4Response.class), any(DependentHostGroupsV4Response.class),
                anyString())).thenCallRealMethod();
        when(dependentHostGroupsService.getDependentHostGroupsForPolicyHostGroups(anyString(), anySet()))
                .thenReturn(getDependentHostGroupsResponse("compute", "master", "gateway1"));
        when(cmCommunicator.isClusterManagerRunning(any(Cluster.class))).thenReturn(false);
        when(stackResponseUtils.primaryGatewayHealthy(any(StackV4Response.class))).thenReturn(true);

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    void testOnApplicationEventWhenStopStartScalingEnabledAndDependentHostsHealthyButPrimaryGatewayUnhealthy() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        cluster.setStopStartScalingEnabled(Boolean.TRUE);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getMockStackResponseWithDependentHostGroup(Status.AVAILABLE, Set.of("master", "gateway1"),
                InstanceStatus.SERVICES_HEALTHY));
        when(stackResponseUtils.getUnhealthyDependentHosts(any(StackV4Response.class), any(DependentHostGroupsV4Response.class),
                anyString())).thenCallRealMethod();
        when(dependentHostGroupsService.getDependentHostGroupsForPolicyHostGroups(anyString(), anySet()))
                .thenReturn(getDependentHostGroupsResponse("compute", "master", "gateway1"));
        when(stackResponseUtils.primaryGatewayHealthy(any(StackV4Response.class))).thenReturn(false);

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    void testOnApplicationEventWhenStopStartScalingEnabledAndCBStatusStopInProgressButCmAndDependentHostsHealthy() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        cluster.setStopStartScalingEnabled(Boolean.TRUE);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getMockStackResponseWithDependentHostGroup(Status.STOP_IN_PROGRESS, Set.of("master",
                "gateway1"), InstanceStatus.SERVICES_HEALTHY));
        when(stackResponseUtils.getUnhealthyDependentHosts(any(StackV4Response.class), any(DependentHostGroupsV4Response.class),
                anyString())).thenCallRealMethod();
        when(dependentHostGroupsService.getDependentHostGroupsForPolicyHostGroups(anyString(), anySet()))
                .thenReturn(getDependentHostGroupsResponse("compute", "master", "gateway1"));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    @Test
    void testOnApplicationEventWhenStopStartScalingEnabledAndCBStatusUpdateInProgressAndCmAndDependentHostGroupsHealthy() {
        Cluster cluster = getACluster(ClusterState.RUNNING);
        cluster.setStopStartScalingEnabled(Boolean.TRUE);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(getMockStackResponseWithDependentHostGroup(Status.UPDATE_IN_PROGRESS, Set.of("master",
                "gateway1"), InstanceStatus.SERVICES_HEALTHY));
        when(stackResponseUtils.getUnhealthyDependentHosts(any(StackV4Response.class), any(DependentHostGroupsV4Response.class),
                anyString())).thenCallRealMethod();
        when(dependentHostGroupsService.getDependentHostGroupsForPolicyHostGroups(anyString(), anySet()))
                .thenReturn(getDependentHostGroupsResponse("compute", "master", "gateway1"));

        underTest.onApplicationEvent(new ClusterStatusSyncEvent(AUTOSCALE_CLUSTER_ID));

        verify(clusterService).setState(AUTOSCALE_CLUSTER_ID, ClusterState.SUSPENDED);
        verify(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
    }

    private DependentHostGroupsV4Response getDependentHostGroupsResponse(String policyHostGroup, String... dependentHostGroups) {
        DependentHostGroupsV4Response response = new DependentHostGroupsV4Response();
        Map<String, Set<String>> dependentHostGroupsMap = Map.of(policyHostGroup, Set.of(dependentHostGroups));
        response.setDependentHostGroups(dependentHostGroupsMap);
        return response;
    }

    private DependentHostGroupsV4Response getUndefinedDependentHostGroupResponse(String policyHostGroup) {
        DependentHostGroupsV4Response response = new DependentHostGroupsV4Response();
        response.setDependentHostGroups(Map.of(policyHostGroup, Set.of("UNDEFINED_DEPENDENCY")));
        return response;
    }

    private Cluster getACluster(ClusterState clusterState) {
        Cluster cluster = new Cluster();
        cluster.setId(AUTOSCALE_CLUSTER_ID);
        cluster.setStackCrn(CLOUDBREAK_STACK_CRN);
        cluster.setState(clusterState);
        cluster.setEnvironmentCrn(TEST_ENVIRONMENT_CRN);
        cluster.setMachineUserCrn("testMachineUser");
        cluster.setStopStartScalingEnabled(Boolean.FALSE);

        ScalingPolicy scalingPolicy = new ScalingPolicy();
        scalingPolicy.setAdjustmentType(AdjustmentType.LOAD_BASED);
        scalingPolicy.setHostGroup("compute");

        LoadAlertConfiguration alertConfiguration = new LoadAlertConfiguration();
        alertConfiguration.setCoolDownMinutes(10);

        LoadAlert loadAlert = new LoadAlert();
        loadAlert.setScalingPolicy(scalingPolicy);
        loadAlert.setLoadAlertConfiguration(alertConfiguration);
        loadAlert.setCluster(cluster);

        cluster.setLoadAlerts(Set.of(loadAlert));

        ClusterPertain clusterPertain = new ClusterPertain();
        clusterPertain.setTenant(TEST_TENANT);
        cluster.setClusterPertain(clusterPertain);
        return cluster;
    }
}