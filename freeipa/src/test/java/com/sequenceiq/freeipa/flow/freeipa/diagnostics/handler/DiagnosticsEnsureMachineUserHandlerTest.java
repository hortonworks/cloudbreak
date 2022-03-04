package com.sequenceiq.freeipa.flow.freeipa.diagnostics.handler;

import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionHandlerSelectors.ENSURE_MACHINE_USER_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.FAILED_DIAGNOSTICS_COLLECTION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
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
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsFlowException;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.freeipa.service.AltusMachineUserService;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
public class DiagnosticsEnsureMachineUserHandlerTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private DiagnosticsEnsureMachineUserHandler underTest;

    @Mock
    private AltusMachineUserService altusMachineUserService;

    @BeforeEach
    public void setUp() {
        underTest = new DiagnosticsEnsureMachineUserHandler();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testDoAccept() throws IOException {
        // GIVEN
        given(altusMachineUserService.getOrCreateDataBusCredentialIfNeeded(anyLong())).willReturn(new DataBusCredential());
        DiagnosticParameters diagnosticParameters = new DiagnosticParameters();
        diagnosticParameters.setDestination(DiagnosticsDestination.SUPPORT);
        // WHEN
        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(ENSURE_MACHINE_USER_EVENT.selector(), STACK_ID, "crn",
                diagnosticParameters);
        underTest.doAccept(new HandlerEvent<>(new Event<>(event)));
        // THEN
        verify(altusMachineUserService, times(1)).getOrCreateDataBusCredentialIfNeeded(anyLong());
    }

    @Test
    public void testDoAcceptOnError() throws IOException {
        // GIVEN
        doThrow(new IOException("ex")).when(altusMachineUserService).getOrCreateDataBusCredentialIfNeeded(anyLong());
        DiagnosticParameters diagnosticParameters = new DiagnosticParameters();
        diagnosticParameters.setDestination(DiagnosticsDestination.SUPPORT);
        // WHEN
        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(ENSURE_MACHINE_USER_EVENT.selector(),
                STACK_ID, "crn", diagnosticParameters);
        DiagnosticsFlowException result = assertThrows(DiagnosticsFlowException.class, () -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))));
        // THEN
        assertTrue(result.getMessage().contains("Error during diagnostics operation: UMS resource check"));
    }

    @Test
    public void testFailureType() {
        assertEquals(UsageProto.CDPVMDiagnosticsFailureType.Value.UMS_RESOURCE_CHECK_FAILURE, underTest.getFailureType());
    }

    @Test
    public void testSelector() {
        assertEquals(ENSURE_MACHINE_USER_EVENT.selector(), underTest.selector());
    }

    @Test
    public void testFailureEvent() {
        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(ENSURE_MACHINE_USER_EVENT.selector(),
                STACK_ID, "crn", new DiagnosticParameters());
        Selectable result = underTest.defaultFailureEvent(STACK_ID, new IllegalArgumentException("ex"), new Event<>(event));
        assertEquals(FAILED_DIAGNOSTICS_COLLECTION_EVENT.selector(), result.selector());
    }

}
