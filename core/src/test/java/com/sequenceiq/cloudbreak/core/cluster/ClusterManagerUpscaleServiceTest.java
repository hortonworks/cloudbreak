package com.sequenceiq.cloudbreak.core.cluster;

import static com.sequenceiq.cloudbreak.polling.ExtendedPollingResult.ExtendedPollingResultBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeReachabilityResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.TargetedUpscaleSupportService;

@ExtendWith(MockitoExtension.class)
public class ClusterManagerUpscaleServiceTest {

    private static final long STACK_ID = 1L;

    private static final long CLUSTER_ID = 2L;

    private static final long INSTANCE_METADATA_ID = 3L;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private ClusterServiceRunner clusterServiceRunner;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private TargetedUpscaleSupportService targetedUpscaleSupportService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private ClusterManagerUpscaleService underTest;

    @Test
    public void testUpscaleIfTargetedUpscaleNotSupportedOrPrimaryGatewayChanged() throws ClusterClientInitException {
        Stack stack = getStack();
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(stack);
        when(clusterService.findOneWithLists(CLUSTER_ID)).thenReturn(Optional.of(stack.getCluster()));
        when(clusterHostServiceRunner.addClusterServices(eq(stack), eq(stack.getCluster()), any(), anyBoolean()))
                .thenReturn(new NodeReachabilityResult(Set.of(), Set.of()));
        doNothing().when(clusterServiceRunner).updateAmbariClientConfig(eq(stack), eq(stack.getCluster()));
        doNothing().when(clusterService).updateInstancesToRunning(eq(STACK_ID), any());
        when(clusterApiConnectors.getConnector(eq(stack))).thenReturn(clusterApi);

        underTest.upscaleClusterManager(STACK_ID, Collections.singletonMap("hg", 1), true, false);

        verifyNoInteractions(targetedUpscaleSupportService);

        when(targetedUpscaleSupportService.targetedUpscaleOperationSupported(any())).thenReturn(Boolean.FALSE);

        underTest.upscaleClusterManager(STACK_ID, Collections.singletonMap("hg", 3), false, false);

        verifyNoMoreInteractions(clusterServiceRunner);
        verify(clusterApi, times(2)).waitForHosts(any());
    }

    @Test
    public void testUpscaleIfTargetedUpscaleSupported() throws ClusterClientInitException {
        Stack stack = getStack();
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(stack);
        when(clusterService.findOneWithLists(eq(CLUSTER_ID))).thenReturn(Optional.of(stack.getCluster()));
        when(clusterHostServiceRunner.addClusterServices(eq(stack), eq(stack.getCluster()), any(), anyBoolean()))
                .thenReturn(new NodeReachabilityResult(Set.of(), Set.of()));
        doNothing().when(clusterService).updateInstancesToRunning(eq(STACK_ID), any());
        when(clusterApiConnectors.getConnector(eq(stack))).thenReturn(clusterApi);
        when(targetedUpscaleSupportService.targetedUpscaleOperationSupported(eq(stack))).thenReturn(Boolean.TRUE);

        underTest.upscaleClusterManager(STACK_ID, Collections.singletonMap("hg", 3), false, false);

        verifyNoInteractions(clusterServiceRunner);
        verify(clusterApi).waitForHosts(any());
    }

    @Test
    public void testUpscaleClusterManagerWhenRepairAndWaitForHostsTimedOut() throws ClusterClientInitException {
        Stack stack = getStack();
        when(stackService.getByIdWithListsInTransaction(eq(STACK_ID))).thenReturn(stack);
        when(clusterService.findOneWithLists(eq(CLUSTER_ID))).thenReturn(Optional.of(stack.getCluster()));
        when(clusterHostServiceRunner.addClusterServices(eq(stack), eq(stack.getCluster()), any(), anyBoolean()))
                .thenReturn(new NodeReachabilityResult(Set.of(), Set.of()));
        doNothing().when(clusterService).updateInstancesToRunning(eq(STACK_ID), any());
        when(clusterApiConnectors.getConnector(eq(stack))).thenReturn(clusterApi);
        when(clusterApi.waitForHosts(any())).thenReturn(new ExtendedPollingResultBuilder().withPayload(Set.of(INSTANCE_METADATA_ID)).timeout().build());

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.upscaleClusterManager(STACK_ID, Collections.singletonMap("hg", 1), false, true));

        verifyNoInteractions(clusterServiceRunner);
        verify(clusterApi).waitForHosts(any());
        verify(instanceMetaDataService, times(1)).updateInstanceStatus(
                eq(Set.of(INSTANCE_METADATA_ID)), eq(InstanceStatus.ORCHESTRATION_FAILED),
                eq("Upscaling cluster manager were not successful, waiting for hosts timed out"));
        assertEquals("Upscaling cluster manager were not successful, waiting for hosts timed out for nodes: [3]", exception.getMessage());
    }

    private Stack getStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        stack.setCluster(cluster);
        return stack;

    }

}
