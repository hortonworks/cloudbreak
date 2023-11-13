package com.sequenceiq.cloudbreak.core.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Memory;
import com.sequenceiq.cloudbreak.orchestrator.model.MemoryInfo;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
class ClusterManagerDefaultConfigAdjusterTest {

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @InjectMocks
    private ClusterManagerDefaultConfigAdjuster clusterManagerDefaultConfigAdjuster;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterView clusterView;

    @Mock
    private GatewayConfig gatewayConfig;

    @Mock
    private ClusterApi clusterApi;

    @ParameterizedTest
    @MethodSource("memoryTestData")
    public void testMemoryAdjustment(int availableMemoryGB, int currentCMMemoryGB, int expectedNewCMMemoryGB, int nodeCount)
            throws CloudbreakOrchestratorException, ClusterClientInitException, CloudbreakException {
        setUpMocks(availableMemoryGB, currentCMMemoryGB);

        clusterManagerDefaultConfigAdjuster.adjustDefaultConfig(stackDto, nodeCount);

        verify(hostOrchestrator).setClusterManagerMemory(gatewayConfig, Memory.ofGigaBytes(expectedNewCMMemoryGB));
        verify(hostOrchestrator).restartClusterManagerOnMaster(
                eq(gatewayConfig),
                eq(Set.of("host.master0.site")),
                any());
        verify(clusterApi).waitForServer(false);
    }

    @Test
    public void testUpdateClusterManagerOperationTimeoutWithHighNodeCount()
            throws CloudbreakOrchestratorException, ClusterClientInitException, CloudbreakException {
        when(stackDto.getType()).thenReturn(StackType.WORKLOAD);
        when(stackDto.getId()).thenReturn(1L);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(gatewayConfig.getHostname()).thenReturn("host.master0.site");
        when(gatewayConfigService.getPrimaryGatewayConfig(stackDto)).thenReturn(gatewayConfig);
        when(hostOrchestrator.getClusterManagerMemory(gatewayConfig))
                .thenReturn(Optional.of(Memory.ofGigaBytes(16)));
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(hostOrchestrator.setClouderaManagerOperationTimeout(gatewayConfig)).thenReturn(true);
        clusterManagerDefaultConfigAdjuster.adjustDefaultConfig(stackDto, 250);
        verify(hostOrchestrator, times(1)).setClouderaManagerOperationTimeout(any());
        verify(hostOrchestrator, times(1)).restartClusterManagerOnMaster(any(), any(), any());
        verify(clusterApi, times(1)).waitForServer(false);
    }

    @Test
    public void testUpdateClusterManagerOperationTimeoutWithSmallNodeCount()
            throws CloudbreakOrchestratorException, ClusterClientInitException, CloudbreakException {
        when(stackDto.getType()).thenReturn(StackType.WORKLOAD);
        when(gatewayConfigService.getPrimaryGatewayConfig(stackDto)).thenReturn(gatewayConfig);
        when(hostOrchestrator.getClusterManagerMemory(gatewayConfig))
                .thenReturn(Optional.of(Memory.ofGigaBytes(16)));
        clusterManagerDefaultConfigAdjuster.adjustDefaultConfig(stackDto, 99);
        verify(hostOrchestrator, times(0)).setClouderaManagerOperationTimeout(any());
        verify(hostOrchestrator, times(0)).restartClusterManagerOnMaster(any(), any(), any());
        verify(clusterApi, times(0)).waitForServer(false);
    }

    @Test
    public void testUpdateClusterManagerOperationTimeoutWithSmallNodeCountButMemoryChangeNeeded()
            throws CloudbreakOrchestratorException, ClusterClientInitException, CloudbreakException {
        setUpMocks(32, 2);
        when(stackDto.getType()).thenReturn(StackType.WORKLOAD);
        clusterManagerDefaultConfigAdjuster.adjustDefaultConfig(stackDto, 99);
        verify(hostOrchestrator, times(0)).setClouderaManagerOperationTimeout(any());
        verify(hostOrchestrator, times(1)).restartClusterManagerOnMaster(any(), any(), any());
        verify(clusterApi, times(1)).waitForServer(false);
    }

    @Test
    public void testMemoryIsAdjustedToTheHighestPossible() throws CloudbreakOrchestratorException, ClusterClientInitException, CloudbreakException {
        testMemoryAdjustment(12, 2, 3, 10);

        verify(cloudbreakEventService).fireCloudbreakEvent(
                1L, "UPDATE_IN_PROGRESS", ResourceEvent.CLUSTER_SCALING_CM_MEMORY_WARNING,
                List.of("4.00", "12.00"));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void testMemoryIsNotChangedIfNoChangeISRequired() throws CloudbreakOrchestratorException {
        setUpMocks(16, 4);

        clusterManagerDefaultConfigAdjuster.adjustDefaultConfig(stackDto, 1);

        verify(hostOrchestrator, never()).setClusterManagerMemory(any(), any());
        verify(hostOrchestrator, never()).restartClusterManagerOnMaster(any(), any(), any());
        verify(cloudbreakEventService, never()).fireCloudbreakEvent(any(), any(), any(), any());
    }

    @Test
    public void testMemoryChangeIsSkippedIfStackIsNotDataHub() throws CloudbreakOrchestratorException {
        when(stackDto.getType()).thenReturn(StackType.DATALAKE);

        clusterManagerDefaultConfigAdjuster.adjustDefaultConfig(stackDto, 1);

        verify(gatewayConfigService, never()).getPrimaryGatewayConfig(any());
        verify(hostOrchestrator, never()).setClusterManagerMemory(any(), any());
        verify(hostOrchestrator, never()).restartClusterManagerOnMaster(any(), any(), any());
        verify(cloudbreakEventService, never()).fireCloudbreakEvent(any(), any(), any(), any());
    }

    private void setUpMocks(int availableMemoryGB, int currentCMMemoryGB) throws CloudbreakOrchestratorFailedException {
        when(stackDto.getType()).thenReturn(StackType.WORKLOAD);
        when(stackDto.getId()).thenReturn(1L);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(gatewayConfig.getHostname()).thenReturn("host.master0.site");
        when(gatewayConfigService.getPrimaryGatewayConfig(stackDto)).thenReturn(gatewayConfig);
        when(hostOrchestrator.getClusterManagerMemory(gatewayConfig))
                .thenReturn(Optional.of(Memory.ofGigaBytes(currentCMMemoryGB)));
        when(hostOrchestrator.getMemoryInfo(gatewayConfig))
                .thenReturn(Optional.of(new MemoryInfo(Map.of("MemTotal", Map.of("value", String.valueOf(availableMemoryGB), "unit", "GB")))));
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
    }

    private static Stream<Arguments> memoryTestData() {
        int availableMemoryGB = 256;
        int currentCMMemoryGB = 2;

        Stream.Builder<Arguments> builder = Stream.builder();
        addTestData(builder, availableMemoryGB, currentCMMemoryGB, 1, 199, 4);
        addTestData(builder, availableMemoryGB, currentCMMemoryGB, 200, 299, 8);
        addTestData(builder, availableMemoryGB, currentCMMemoryGB, 300, 399, 12);
        addTestData(builder, availableMemoryGB, currentCMMemoryGB, 400, 499, 16);
        addTestData(builder, availableMemoryGB, currentCMMemoryGB, 500, 599, 20);
        addTestData(builder, availableMemoryGB, currentCMMemoryGB, 600, 699, 24);
        addTestData(builder, availableMemoryGB, currentCMMemoryGB, 700, 799, 28);
        addTestData(builder, availableMemoryGB, currentCMMemoryGB, 800, 950, 32);
        return builder.build();
    }

    private static void addTestData(Stream.Builder<Arguments> builder,
            int availableMemoryGB,
            int currentCMMemoryGB,
            int startNodeCount,
            int endNodeCount,
            int expectedCMMemoryGB) {
        for (int nodeCount = startNodeCount; nodeCount <= endNodeCount; nodeCount++) {
            builder.add(Arguments.of(availableMemoryGB, currentCMMemoryGB, expectedCMMemoryGB, nodeCount));
        }
    }
}