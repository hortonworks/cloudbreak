package com.sequenceiq.freeipa.flow.freeipa.diagnostics.handler;

import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionHandlerSelectors.UPGRADE_DIAGNOSTICS_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.FAILED_DIAGNOSTICS_COLLECTION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.telemetry.upgrade.TelemetryUpgradeService;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsFlowException;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionEvent;

@ExtendWith(MockitoExtension.class)
public class DiagnosticsUpgradeTelemetryHandlerTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private DiagnosticsUpgradeTelemetryHandler underTest;

    @Mock
    private TelemetryUpgradeService telemetryUpgradeService;

    @BeforeEach
    public void setUp() {
        underTest = new DiagnosticsUpgradeTelemetryHandler();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testDoAccept() throws CloudbreakOrchestratorFailedException {
        // GIVEN
        doNothing().when(telemetryUpgradeService).upgradeTelemetryComponent(anyLong(), any(), any());
        // WHEN
        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(UPGRADE_DIAGNOSTICS_EVENT.selector(), STACK_ID, "crn",
                new DiagnosticParameters());
        underTest.doAccept(new HandlerEvent<>(new Event<>(event)));
        // THEN
        verify(telemetryUpgradeService, times(1)).upgradeTelemetryComponent(anyLong(), any(), any());
    }

    @Test
    public void testDoAcceptOnError() throws CloudbreakOrchestratorFailedException {
        // GIVEN
        doThrow(new CloudbreakOrchestratorFailedException("ex")).when(telemetryUpgradeService).upgradeTelemetryComponent(anyLong(), any(), any());
        // WHEN
        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(UPGRADE_DIAGNOSTICS_EVENT.selector(),
                STACK_ID, "crn", new DiagnosticParameters());
        DiagnosticsFlowException result = assertThrows(DiagnosticsFlowException.class, () -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))));
        // THEN
        assertTrue(result.getMessage().contains("Error during diagnostics operation: Upgrade telemetry"));
    }

    @Test
    public void testFailureType() {
        assertEquals(UsageProto.CDPVMDiagnosticsFailureType.Value.TELEMETRY_UPGRADE_FAILURE, underTest.getFailureType());
    }

    @Test
    public void testSelector() {
        assertEquals(UPGRADE_DIAGNOSTICS_EVENT.selector(), underTest.selector());
    }

    @Test
    public void testFailureEvent() {
        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(UPGRADE_DIAGNOSTICS_EVENT.selector(),
                STACK_ID, "crn", new DiagnosticParameters());
        Selectable result = underTest.defaultFailureEvent(STACK_ID, new IllegalArgumentException("ex"), new Event<>(event));
        assertEquals(FAILED_DIAGNOSTICS_COLLECTION_EVENT.selector(), result.selector());
    }
}
