package com.sequenceiq.periscope.monitor.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.service.AltusMachineUserService;
import com.sequenceiq.periscope.service.ClusterService;

@ExtendWith(MockitoExtension.class)
class ClusterDeleteHandlerTest {

    private static final Long AUTOSCALE_CLUSTER_ID = 1L;

    private static final String CLOUDBREAK_STACK_CRN = "crn:cdp:datahub:us-west-1:tenant:datahub:012e39ab-e972-4345-87ee-0e12b47aa5b8";

    private static final String CLOUDBREAK_ENV_CRN = "crn:cdp:environments:us-west-1:default:environment:da337ac7-82ef-4f6c-a13c-45aa6960282d";

    private static final String MACHINE_USER_CRN = "test_machine";

    private static final String TENANT = "tenant";

    @Mock
    private ClusterService clusterService;

    @Mock
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Mock
    private AltusMachineUserService altusMachineUserService;

    @Mock
    private ExecutorService deleteExecutorService;

    @InjectMocks
    private ClusterDeleteHandler underTest;

    @Test
    void testDeleteClustersWhenClusterDoesNotExist() {
        Long since = 100L;
        when(cloudbreakCommunicator.getDeletedClusters(since)).thenReturn(getDeletedStacks());
        when(clusterService.findOneByStackCrn(CLOUDBREAK_STACK_CRN)).thenReturn(Optional.empty());

        underTest.deleteClusters(since);

        verify(cloudbreakCommunicator).getDeletedClusters(since);
        verify(deleteExecutorService, never()).submit(any(Callable.class));
    }

    @Test
    void testDeleteClustersWhenClusterReturnedFromCBButNoRetryCluster() {
        Long since = 100L;
        Future<String> future = mock(Future.class);
        when(cloudbreakCommunicator.getDeletedClusters(since)).thenReturn(getDeletedStacks());
        when(clusterService.findOneByStackCrn(CLOUDBREAK_STACK_CRN)).thenReturn(Optional.of(getCluster(ClusterState.RUNNING)));
        when(future.isDone()).thenReturn(true);
        when(deleteExecutorService.submit(any(Callable.class))).thenReturn(future);

        underTest.deleteClusters(since);

        verify(cloudbreakCommunicator).getDeletedClusters(since);
        verify(clusterService).findOneByStackCrn(CLOUDBREAK_STACK_CRN);
        verify(deleteExecutorService).submit(any(Callable.class));
    }

    @Test
    void testDeleteClustersWhenClusterReturnedFromCBButAlongWithRetryCluster() {
        Long since = 100L;
        ReflectionTestUtils.setField(underTest, "maxDeleteRetryCount", 5);
        Future<String> future = mock(Future.class);
        when(cloudbreakCommunicator.getDeletedClusters(since)).thenReturn(getDeletedStacks());
        when(clusterService.findOneByStackCrn(CLOUDBREAK_STACK_CRN)).thenReturn(Optional.of(getCluster(ClusterState.RUNNING)));
        when(clusterService.findByDeleteRetryCount(5)).thenReturn(List.of(getCluster(ClusterState.RUNNING), getCluster(ClusterState.RUNNING)));
        when(future.isDone()).thenReturn(true);
        when(deleteExecutorService.submit(any(Callable.class))).thenReturn(future);

        underTest.deleteClusters(since);

        verify(cloudbreakCommunicator).getDeletedClusters(since);
        verify(clusterService).findOneByStackCrn(CLOUDBREAK_STACK_CRN);
        verify(deleteExecutorService, times(3)).submit(any(Callable.class));
    }

    @Test
    void testDeleteCluster() {
        when(clusterService.countByEnvironmentCrn(CLOUDBREAK_ENV_CRN)).thenReturn(Integer.valueOf(1));

        underTest.deleteCluster(getCluster(ClusterState.RUNNING));

        verify(clusterService).removeById(AUTOSCALE_CLUSTER_ID);
        verify(altusMachineUserService).deleteMachineUserForEnvironment(TENANT, MACHINE_USER_CRN, CLOUDBREAK_ENV_CRN);
    }

    @Test
    void testDeleteClusterButMachineUserNotDeleted() {

        when(clusterService.countByEnvironmentCrn(CLOUDBREAK_ENV_CRN)).thenReturn(Integer.valueOf(2));

        underTest.deleteCluster(getCluster(ClusterState.RUNNING));

        verify(clusterService).removeById(AUTOSCALE_CLUSTER_ID);
        verify(altusMachineUserService, never()).deleteMachineUserForEnvironment(anyString(), anyString(), anyString());
    }

    @Test
    void testDeleteClusterFailedRetryCountNull() {
        when(clusterService.countByEnvironmentCrn(CLOUDBREAK_ENV_CRN)).thenReturn(Integer.valueOf(1));
        doThrow(new RuntimeException()).when(clusterService).removeById(AUTOSCALE_CLUSTER_ID);

        underTest.deleteCluster(getCluster(ClusterState.RUNNING));
        verify(clusterService).updateClusterDeleted(AUTOSCALE_CLUSTER_ID, ClusterState.DELETED, 1);
    }

    @Test
    void testDeleteClusterFailedRetryCountNotNull() {
        Cluster cluster = getCluster(ClusterState.RUNNING);
        cluster.setDeleteRetryCount(2);
        when(clusterService.countByEnvironmentCrn(CLOUDBREAK_ENV_CRN)).thenReturn(Integer.valueOf(1));
        doThrow(new RuntimeException()).when(clusterService).removeById(AUTOSCALE_CLUSTER_ID);

        underTest.deleteCluster(cluster);
        verify(clusterService).updateClusterDeleted(AUTOSCALE_CLUSTER_ID, ClusterState.DELETED, 3);
    }

    private Cluster getCluster(ClusterState clusterState) {
        Cluster cluster = new Cluster();
        cluster.setId(AUTOSCALE_CLUSTER_ID);
        cluster.setStackCrn(CLOUDBREAK_STACK_CRN);
        cluster.setState(clusterState);
        cluster.setStopStartScalingEnabled(Boolean.FALSE);
        cluster.setEnvironmentCrn(CLOUDBREAK_ENV_CRN);
        cluster.setMachineUserCrn(MACHINE_USER_CRN);

        ClusterPertain clusterPertain = new ClusterPertain();
        clusterPertain.setTenant(TENANT);
        cluster.setClusterPertain(clusterPertain);
        return cluster;
    }

    private List<StackStatusV4Response> getDeletedStacks() {
        StackStatusV4Response stackStatusV4Response = new StackStatusV4Response();
        stackStatusV4Response.setCrn(CLOUDBREAK_STACK_CRN);
        return List.of(stackStatusV4Response);
    }
}