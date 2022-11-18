package com.sequenceiq.cloudbreak.core.flow2.diagnostics.handler;

import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionHandlerSelectors.PREFLIGHT_CHECK_DIAGNOSTICS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionHandlerSelectors.SALT_PILLAR_UPDATE_DIAGNOSTICS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionStateSelectors.FAILED_DIAGNOSTICS_COLLECTION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.DiagnosticsFlowException;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.DiagnosticsFlowService;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class DiagnosticsPreFlightCheckHandlerTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private DiagnosticsPreFlightCheckHandler underTest;

    @Mock
    private DiagnosticsFlowService diagnosticsFlowService;

    @BeforeEach
    public void setUp() {
        underTest = new DiagnosticsPreFlightCheckHandler();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testDoAccept() {
        // GIVEN
        doNothing().when(diagnosticsFlowService).nodeStatusNetworkReport(anyLong());
        doNothing().when(diagnosticsFlowService).nodeStatusMeteringReport(anyLong());
        // WHEN
        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(SALT_PILLAR_UPDATE_DIAGNOSTICS_EVENT.selector(), STACK_ID, "crn",
                new DiagnosticParameters(), Set.of(), Set.of(), Set.of());
        underTest.doAccept(new HandlerEvent<>(new Event<>(event)));
        // THEN
        verify(diagnosticsFlowService, times(1)).nodeStatusNetworkReport(anyLong());
    }

    @Test
    public void testDoAcceptOnError() {
        // GIVEN
        doThrow(new IllegalArgumentException("ex")).when(diagnosticsFlowService).nodeStatusNetworkReport(anyLong());
        // WHEN
        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(SALT_PILLAR_UPDATE_DIAGNOSTICS_EVENT.selector(),
                STACK_ID, "crn", new DiagnosticParameters(), Set.of(), Set.of(), Set.of());
        DiagnosticsFlowException result = assertThrows(DiagnosticsFlowException.class, () -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))));
        // THEN
        assertTrue(result.getMessage().contains("Error during diagnostics operation: Pre-flight check"));
    }

    @Test
    public void testFailureType() {
        assertEquals(UsageProto.CDPVMDiagnosticsFailureType.Value.UNSET, underTest.getFailureType());
    }

    @Test
    public void testSelector() {
        assertEquals(PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector(), underTest.selector());
    }

    @Test
    public void testFailureEvent() {
        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector(),
                STACK_ID, "crn", new DiagnosticParameters(), Set.of(), Set.of(), Set.of());
        Selectable result = underTest.defaultFailureEvent(STACK_ID, new IllegalArgumentException("ex"), new Event<>(event));
        assertEquals(FAILED_DIAGNOSTICS_COLLECTION_EVENT.selector(), result.selector());
    }
}
