package com.sequenceiq.cloudbreak.core.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
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
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeReachabilityResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.TargetedUpscaleSupportService;

@ExtendWith(MockitoExtension.class)
public class ClusterManagerUpscaleServiceTest {

    @Mock
    private StackDtoService stackDtoService;

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
        StackDto stack = getStack();
        when(stackDtoService.getById(any())).thenReturn(stack);
        when(stack.getClusterManagerIp()).thenReturn("otherclusterIp");
        when(clusterHostServiceRunner.addClusterServices(any(), any(), anyBoolean())).thenReturn(new NodeReachabilityResult(Set.of(), Set.of()));
        when(clusterServiceRunner.updateAmbariClientConfig(stack)).thenReturn("clusterIp");
        doNothing().when(clusterService).updateInstancesToRunning(any(), any());
        when(clusterApiConnectors.getConnector(stack, "clusterIp")).thenReturn(clusterApi);

        underTest.upscaleClusterManager(1L, Collections.singletonMap("hg", 1), true, false);

        verifyNoInteractions(targetedUpscaleSupportService);

        when(targetedUpscaleSupportService.targetedUpscaleOperationSupported(any())).thenReturn(Boolean.FALSE);

        when(clusterApiConnectors.getConnector(stack, "otherclusterIp")).thenReturn(clusterApi);

        underTest.upscaleClusterManager(1L, Collections.singletonMap("hg", 1), false, false);

        verifyNoMoreInteractions(clusterServiceRunner);
        verify(clusterApi, times(2)).waitForHosts(any());
    }

    @Test
    public void testUpscaleIfTargetedUpscaleSupported() throws ClusterClientInitException {
        StackDto stack = getStack();
        when(stackDtoService.getById(any())).thenReturn(stack);
        when(stack.getClusterManagerIp()).thenReturn("otherclusterIp");
        when(clusterHostServiceRunner.addClusterServices(any(), any(), anyBoolean())).thenReturn(new NodeReachabilityResult(Set.of(), Set.of()));
        doNothing().when(clusterService).updateInstancesToRunning(any(), any());
        when(clusterApiConnectors.getConnector(stack, "otherclusterIp")).thenReturn(clusterApi);
        when(targetedUpscaleSupportService.targetedUpscaleOperationSupported(any())).thenReturn(Boolean.TRUE);

        underTest.upscaleClusterManager(1L, Collections.singletonMap("hg", 1), false, false);

        verifyNoInteractions(clusterServiceRunner);
        verify(clusterApi).waitForHosts(any());
    }

    private StackDto getStack() {
        StackDto stackDto = mock(StackDto.class);
        Stack stack = new Stack();
        when(stackDto.getStack()).thenReturn(stack);
        return stackDto;

    }

}
