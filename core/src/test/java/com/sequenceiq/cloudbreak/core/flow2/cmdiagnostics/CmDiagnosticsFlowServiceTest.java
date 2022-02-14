package com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class CmDiagnosticsFlowServiceTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private CmDiagnosticsFlowService underTest;

    @Mock
    private StackService stackService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private TelemetryOrchestrator telemetryOrchestrator;

    @BeforeEach
    public void setUp() {
        underTest = new CmDiagnosticsFlowService();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testInit() throws CloudbreakOrchestratorFailedException {
        // GIVEN
        Stack stack = mock(Stack.class);
        given(stackService.getByIdWithListsInTransaction(STACK_ID)).willReturn(stack);
        given(stack.getCluster()).willReturn(mock(Cluster.class));
        given(stack.getAllNodes()).willReturn(nodes());
        given(gatewayConfigService.getAllGatewayConfigs(stack)).willReturn(new ArrayList<>());
        given(gatewayConfigService.getPrimaryGatewayIp(stack)).willReturn("hostA");
        doNothing().when(telemetryOrchestrator).initDiagnosticCollection(any(), anySet(), any(), any());
        // WHEN
        underTest.init(STACK_ID, Map.of(), Set.of());
        // THEN
        verify(telemetryOrchestrator, times(1)).initDiagnosticCollection(any(), anySet(), any(), any());
    }

    @Test
    public void testInitWithCloudbreakOrchestratorError() throws CloudbreakOrchestratorFailedException {
        // GIVEN
        Stack stack = mock(Stack.class);
        given(stackService.getByIdWithListsInTransaction(STACK_ID)).willReturn(stack);
        given(stack.getCluster()).willReturn(mock(Cluster.class));
        given(stack.getAllNodes()).willReturn(nodes());
        given(gatewayConfigService.getAllGatewayConfigs(stack)).willReturn(new ArrayList<>());
        given(gatewayConfigService.getPrimaryGatewayIp(stack)).willReturn("hostA");
        doThrow(new CloudbreakOrchestratorFailedException("ex")).when(telemetryOrchestrator).initDiagnosticCollection(any(), anySet(), any(), any());
        // WHEN
        CloudbreakOrchestratorFailedException result = assertThrows(
                CloudbreakOrchestratorFailedException.class, () -> underTest.init(STACK_ID, Map.of(), Set.of()));
        // THEN
        assertEquals("ex", result.getMessage());
    }

    @Test
    public void testInitWithoutNodes() throws CloudbreakOrchestratorFailedException {
        // GIVEN
        Stack stack = mock(Stack.class);
        given(stackService.getByIdWithListsInTransaction(STACK_ID)).willReturn(stack);
        given(stack.getCluster()).willReturn(mock(Cluster.class));
        given(stack.getAllNodes()).willReturn(Set.of());
        given(gatewayConfigService.getAllGatewayConfigs(stack)).willReturn(new ArrayList<>());
        given(gatewayConfigService.getPrimaryGatewayIp(stack)).willReturn("hostA");
        // WHEN
        underTest.init(STACK_ID, Map.of(), Set.of());
        // THEN
        verify(telemetryOrchestrator, times(0)).initDiagnosticCollection(any(), anySet(), any(), any());
    }

    @Test
    public void testUpload() throws CloudbreakOrchestratorFailedException {
        // GIVEN
        Stack stack = mock(Stack.class);
        given(stackService.getByIdWithListsInTransaction(STACK_ID)).willReturn(stack);
        given(stack.getCluster()).willReturn(mock(Cluster.class));
        given(stack.getAllNodes()).willReturn(nodes());
        given(gatewayConfigService.getAllGatewayConfigs(stack)).willReturn(new ArrayList<>());
        given(gatewayConfigService.getPrimaryGatewayIp(stack)).willReturn("hostA");
        doNothing().when(telemetryOrchestrator).uploadCollectedDiagnostics(any(), anySet(), any(), any());
        // WHEN
        underTest.upload(STACK_ID, Map.of(), Set.of());
        // THEN
        verify(telemetryOrchestrator, times(1)).uploadCollectedDiagnostics(any(), anySet(), any(), any());
    }

    @Test
    public void testCleanup() throws CloudbreakOrchestratorFailedException {
        // GIVEN
        Stack stack = mock(Stack.class);
        given(stackService.getByIdWithListsInTransaction(STACK_ID)).willReturn(stack);
        given(stack.getCluster()).willReturn(mock(Cluster.class));
        given(stack.getAllNodes()).willReturn(nodes());
        given(gatewayConfigService.getAllGatewayConfigs(stack)).willReturn(new ArrayList<>());
        given(gatewayConfigService.getPrimaryGatewayIp(stack)).willReturn("hostA");
        doNothing().when(telemetryOrchestrator).cleanupCollectedDiagnostics(any(), anySet(), any(), any());
        // WHEN
        underTest.cleanup(STACK_ID, Map.of(), Set.of());
        // THEN
        verify(telemetryOrchestrator, times(1)).cleanupCollectedDiagnostics(any(), anySet(), any(), any());
    }

    private Set<Node> nodes() {
        Set<Node> nodes = new HashSet<>();
        nodes.add(new Node("hostA", "publicHostA", null, null, "hostA", null, null));
        nodes.add(new Node("hostB", "publicHostB", null, null, "hostB", null, null));
        return nodes;
    }
}
