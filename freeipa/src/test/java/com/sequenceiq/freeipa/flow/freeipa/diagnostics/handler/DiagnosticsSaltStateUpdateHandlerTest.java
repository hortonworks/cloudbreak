package com.sequenceiq.freeipa.flow.freeipa.diagnostics.handler;

import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionHandlerSelectors.SALT_STATE_UPDATE_DIAGNOSTICS_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.FAILED_DIAGNOSTICS_COLLECTION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

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
public class DiagnosticsSaltStateUpdateHandlerTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private DiagnosticsSaltStateUpdateHandler underTest;

    @Mock
    private TelemetryUpgradeService telemetryUpgradeService;

    @BeforeEach
    public void setUp() {
        underTest = new DiagnosticsSaltStateUpdateHandler();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testDoAccept() throws CloudbreakOrchestratorFailedException, IOException {
        // GIVEN
        doNothing().when(telemetryUpgradeService).upgradeTelemetrySaltStates(anyLong(), anySet());
        // WHEN
        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(SALT_STATE_UPDATE_DIAGNOSTICS_EVENT.selector(), STACK_ID, "crn",
                new DiagnosticParameters());
        underTest.doAccept(new HandlerEvent<>(new Event<>(event)));
        // THEN
        verify(telemetryUpgradeService, times(1)).upgradeTelemetrySaltStates(anyLong(), anySet());
    }

    @Test
    public void testDoAcceptOnError() throws CloudbreakOrchestratorFailedException, IOException {
        // GIVEN
        doThrow(new CloudbreakOrchestratorFailedException("ex")).when(telemetryUpgradeService).upgradeTelemetrySaltStates(anyLong(), anySet());
        // WHEN
        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(SALT_STATE_UPDATE_DIAGNOSTICS_EVENT.selector(),
                STACK_ID, "crn", new DiagnosticParameters());
        DiagnosticsFlowException result = assertThrows(DiagnosticsFlowException.class, () -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))));
        // THEN
        assertTrue(result.getMessage().contains("Error during diagnostics operation: Salt state update"));
    }

    @Test
    public void testFailureType() {
        assertEquals(UsageProto.CDPVMDiagnosticsFailureType.Value.SALT_STATE_UPDATE_FAILURE, underTest.getFailureType());
    }

    @Test
    public void testSelector() {
        assertEquals(SALT_STATE_UPDATE_DIAGNOSTICS_EVENT.selector(), underTest.selector());
    }

    @Test
    public void testFailureEvent() {
        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(SALT_STATE_UPDATE_DIAGNOSTICS_EVENT.selector(),
                STACK_ID, "crn", new DiagnosticParameters());
        Selectable result = underTest.defaultFailureEvent(STACK_ID, new IllegalArgumentException("ex"), new Event<>(event));
        assertEquals(FAILED_DIAGNOSTICS_COLLECTION_EVENT.selector(), result.selector());
    }
}
