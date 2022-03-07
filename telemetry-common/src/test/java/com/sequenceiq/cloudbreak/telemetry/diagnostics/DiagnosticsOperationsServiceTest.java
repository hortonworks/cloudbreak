package com.sequenceiq.cloudbreak.telemetry.diagnostics;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadata;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataProvider;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;

@ExtendWith(MockitoExtension.class)
public class DiagnosticsOperationsServiceTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private DiagnosticsOperationsService underTest;

    @Mock
    private TelemetryOrchestrator telemetryOrchestrator;

    @Mock
    private UsageReporter usageReporter;

    @Mock
    private OrchestratorMetadataProvider orchestratorMetadataProvider;

    @Mock
    private OrchestratorMetadata metadata;

    @BeforeEach
    public void setUp() {
        underTest = new DiagnosticsOperationsService();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testInit() throws Exception {
        // GIVEN
        given(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).willReturn(metadata);
        given(metadata.getNodes()).willReturn(nodes());
        doNothing().when(telemetryOrchestrator).initDiagnosticCollection(anyList(), anySet(), anyMap(), any());
        // WHEN
        underTest.init(STACK_ID, diagnosticParameters(DiagnosticsDestination.SUPPORT));
        // THEN
        verify(telemetryOrchestrator, times(1)).initDiagnosticCollection(anyList(), anySet(), anyMap(), any());
    }

    @Test
    public void testInitWithoutNodes() throws Exception {
        // GIVEN
        given(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).willReturn(metadata);
        given(metadata.getNodes()).willReturn(new HashSet<>());
        // WHEN
        underTest.init(STACK_ID, diagnosticParameters(DiagnosticsDestination.SUPPORT));
        // THEN
        verify(telemetryOrchestrator, times(0)).initDiagnosticCollection(anyList(), anySet(), anyMap(), any());
    }

    @Test
    public void testCollect() throws Exception {
        // GIVEN
        given(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).willReturn(metadata);
        given(metadata.getNodes()).willReturn(nodes());
        doNothing().when(telemetryOrchestrator).executeDiagnosticCollection(anyList(), anySet(), anyMap(), any());
        // WHEN
        underTest.collect(STACK_ID, diagnosticParameters(DiagnosticsDestination.SUPPORT));
        // THEN
        verify(telemetryOrchestrator, times(1)).executeDiagnosticCollection(anyList(), anySet(), anyMap(), any());
    }

    @Test
    public void testUpload() throws Exception {
        // GIVEN
        given(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).willReturn(metadata);
        given(metadata.getNodes()).willReturn(nodes());
        doNothing().when(telemetryOrchestrator).uploadCollectedDiagnostics(anyList(), anySet(), anyMap(), any());
        // WHEN
        underTest.upload(STACK_ID, diagnosticParameters(DiagnosticsDestination.SUPPORT));
        // THEN
        verify(telemetryOrchestrator, times(1)).uploadCollectedDiagnostics(anyList(), anySet(), anyMap(), any());
    }

    @Test
    public void testCleanup() throws Exception {
        // GIVEN
        given(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).willReturn(metadata);
        given(metadata.getNodes()).willReturn(nodes());
        doNothing().when(telemetryOrchestrator).cleanupCollectedDiagnostics(anyList(), anySet(), anyMap(), any());
        // WHEN
        underTest.cleanup(STACK_ID, diagnosticParameters(DiagnosticsDestination.SUPPORT));
        // THEN
        verify(telemetryOrchestrator, times(1)).cleanupCollectedDiagnostics(anyList(), anySet(), anyMap(), any());
    }

    @Test
    public void vmPreflightCheck() throws Exception {
        // GIVEN
        given(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).willReturn(metadata);
        given(metadata.getNodes()).willReturn(nodes());
        doNothing().when(telemetryOrchestrator).preFlightDiagnosticsCheck(anyList(), anySet(), anyMap(), any());
        // WHEN
        underTest.vmPreflightCheck(STACK_ID, diagnosticParameters(DiagnosticsDestination.SUPPORT));
        // THEN
        verify(telemetryOrchestrator, times(1)).preFlightDiagnosticsCheck(anyList(), anySet(), anyMap(), any());
    }

    @Test
    public void applyUnresponsiveNodesCheck() throws Exception {
        // GIVEN
        given(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).willReturn(metadata);
        given(metadata.getNodes()).willReturn(nodes());
        given(telemetryOrchestrator.collectUnresponsiveNodes(anyList(), anySet(), any())).willReturn(new HashSet<>());
        // WHEN
        underTest.applyUnresponsiveHosts(STACK_ID, diagnosticParameters(DiagnosticsDestination.SUPPORT));
        // THEN
        verify(telemetryOrchestrator, times(1)).collectUnresponsiveNodes(anyList(), anySet(), any());
    }

    @Test
    public void applyUnresponsiveNodesCheckWithUnresponsiveNode() throws Exception {
        // GIVEN
        given(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).willReturn(metadata);
        given(metadata.getNodes()).willReturn(nodes());
        given(telemetryOrchestrator.collectUnresponsiveNodes(anyList(), anySet(), any())).willReturn(nodes());
        // WHEN
        CloudbreakOrchestratorFailedException ex = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.applyUnresponsiveHosts(STACK_ID, diagnosticParameters(DiagnosticsDestination.SUPPORT)));
        // THEN
        assertTrue(ex.getMessage().contains("Some of the hosts are unresponsive"));
    }

    @Test
    public void applyUnresponsiveNodesCheckWithSkipUnresponsiveNodes() throws Exception {
        // GIVEN
        given(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).willReturn(metadata);
        given(metadata.getNodes()).willReturn(nodes());
        given(telemetryOrchestrator.collectUnresponsiveNodes(anyList(), anySet(), any())).willReturn(nodes());
        DiagnosticParameters params = diagnosticParameters(DiagnosticsDestination.SUPPORT);
        params.setSkipUnresponsiveHosts(true);
        // WHEN
        DiagnosticParameters result = underTest.applyUnresponsiveHosts(STACK_ID, params);
        // THEN
        assertTrue(result.getExcludeHosts().contains("host1"));
    }

    @Test
    public void testVmDiagnosticsReport() {
        // GIVEN
        doNothing().when(usageReporter).cdpVmDiagnosticsEvent(any());
        // WHEN
        underTest.vmDiagnosticsReport("crn", diagnosticParameters(DiagnosticsDestination.SUPPORT));
        // THEN
        verify(usageReporter, times(1)).cdpVmDiagnosticsEvent(any());
    }

    @Test
    public void testVmDiagnosticsReportWithFailureType() {
        // GIVEN
        doNothing().when(usageReporter).cdpVmDiagnosticsEvent(any());
        // WHEN
        underTest.vmDiagnosticsReport("crn", diagnosticParameters(DiagnosticsDestination.SUPPORT),
                UsageProto.CDPVMDiagnosticsFailureType.Value.INITIALIZATION_FAILURE, new IllegalArgumentException("ex"));
        // THEN
        verify(usageReporter, times(1)).cdpVmDiagnosticsEvent(any());
    }

    @Test
    public void testVmDiagnosticsReportWithCloudStorageDestination() {
        // GIVEN
        doNothing().when(usageReporter).cdpVmDiagnosticsEvent(any());
        // WHEN
        underTest.vmDiagnosticsReport("crn", diagnosticParameters(DiagnosticsDestination.CLOUD_STORAGE));
        // THEN
        verify(usageReporter, times(1)).cdpVmDiagnosticsEvent(any());
    }

    @Test
    public void testVmDiagnosticsReportWithEngDestination() {
        // GIVEN
        doNothing().when(usageReporter).cdpVmDiagnosticsEvent(any());
        // WHEN
        underTest.vmDiagnosticsReport("crn", diagnosticParameters(DiagnosticsDestination.ENG));
        // THEN
        verify(usageReporter, times(1)).cdpVmDiagnosticsEvent(any());
    }

    @Test
    public void testVmDiagnosticsReportWithLocalDestination() {
        // GIVEN
        doNothing().when(usageReporter).cdpVmDiagnosticsEvent(any());
        // WHEN
        underTest.vmDiagnosticsReport("crn", diagnosticParameters(DiagnosticsDestination.LOCAL));
        // THEN
        verify(usageReporter, times(1)).cdpVmDiagnosticsEvent(any());
    }

    @Test
    public void testVmDiagnosticsReportWithDefaultDestination() {
        // GIVEN
        doNothing().when(usageReporter).cdpVmDiagnosticsEvent(any());
        // WHEN
        underTest.vmDiagnosticsReport("crn", diagnosticParameters(null));
        // THEN
        verify(usageReporter, times(1)).cdpVmDiagnosticsEvent(any());
    }

    private DiagnosticParameters diagnosticParameters(DiagnosticsDestination destination) {
        DiagnosticParameters result = new DiagnosticParameters();
        result.setStatusReason("status");
        if (destination != null) {
            result.setDestination(DiagnosticsDestination.SUPPORT);
        }
        return result;
    }

    private Set<Node> nodes() {
        Set<Node> nodes = new HashSet<>();
        nodes.add(new Node("host1", null, null, null, "host1", null, null));
        return nodes;
    }
}
