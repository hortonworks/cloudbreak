package com.sequenceiq.cloudbreak.core.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.TargetedUpscaleSupportService;

@ExtendWith(MockitoExtension.class)
public class ClusterManagerUpscaleServiceTest {

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

    @InjectMocks
    private ClusterManagerUpscaleService underTest;

    @Test
    public void testUpscaleIfTargetedUpscaleNotSupportedOrPrimaryGatewayChanged() throws ClusterClientInitException {
        when(stackService.getByIdWithListsInTransaction(any())).thenReturn(getStack());
        when(clusterHostServiceRunner.addClusterServices(any(), any(), any())).thenReturn(Map.of());
        doNothing().when(clusterServiceRunner).updateAmbariClientConfig(any(), any());
        doNothing().when(clusterService).updateInstancesToRunning(any(), any());
        when(clusterApiConnectors.getConnector(any(Stack.class))).thenReturn(clusterApi);

        underTest.upscaleClusterManager(1L, "hg", 1, true);

        verifyNoInteractions(targetedUpscaleSupportService);

        when(targetedUpscaleSupportService.targetedUpscaleOperationSupported(any())).thenReturn(Boolean.FALSE);

        underTest.upscaleClusterManager(1L, "hg", 1, false);

        verifyNoMoreInteractions(clusterServiceRunner);
        verify(clusterHostServiceRunner, times(0)).getReachableCandidates(any(), any());
        verify(clusterApi, times(2)).waitForHosts(any());
    }

    @Test
    public void testUpscaleIfTargetedUpscaleSupported() throws ClusterClientInitException {
        when(stackService.getByIdWithListsInTransaction(any())).thenReturn(getStack());
        when(clusterHostServiceRunner.addClusterServices(any(), any(), any())).thenReturn(Map.of());
        doNothing().when(clusterService).updateInstancesToRunning(any(), any());
        when(clusterApiConnectors.getConnector(any(Stack.class))).thenReturn(clusterApi);
        when(targetedUpscaleSupportService.targetedUpscaleOperationSupported(any())).thenReturn(Boolean.TRUE);
        when(clusterHostServiceRunner.getReachableCandidates(any(), any())).thenReturn(Set.of());

        underTest.upscaleClusterManager(1L, "hg", 1, false);

        verifyNoInteractions(clusterServiceRunner);
        verify(clusterApi).waitForHosts(any());
    }

    private Stack getStack() {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        stack.setCluster(cluster);
        return stack;

    }

}
