package com.sequenceiq.cloudbreak.core.flow2.diagnostics.handler;

import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionHandlerSelectors.PREFLIGHT_CHECK_DIAGNOSTICS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionStateSelectors.FAILED_DIAGNOSTICS_COLLECTION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_INIT_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.DiagnosticsFlowException;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.DiagnosticsFlowService;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class DiagnosticsPreFlightCheckHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String RESOURCE_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:12345";

    @InjectMocks
    private DiagnosticsPreFlightCheckHandler underTest;

    @Mock
    private DiagnosticsFlowService diagnosticsFlowService;

    @Mock
    private StackService stackService;

    @Test
    public void testExecuteOperationSuccess() throws Exception {
        // GIVEN
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        DiagnosticParameters parameters = new DiagnosticParameters();
        parameters.setHosts(Set.of("host1"));
        parameters.setHostGroups(Set.of("hostgroup1"));
        parameters.setExcludeHosts(Set.of("excludehost1"));

        doNothing().when(diagnosticsFlowService).nodeStatusNetworkReport(any(Stack.class));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);

        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector(),
                STACK_ID, RESOURCE_CRN, parameters, Set.of("host1"), Set.of("hostgroup1"), Set.of("excludehost1"));

        // WHEN
        Selectable result = underTest.executeOperation(new HandlerEvent<>(new Event<>(event)));

        // THEN
        verify(diagnosticsFlowService, times(1)).nodeStatusNetworkReport(stack);
        verify(stackService, times(1)).getByIdWithListsInTransaction(STACK_ID);

        assertEquals(START_DIAGNOSTICS_INIT_EVENT.selector(), result.selector());
        assertEquals(STACK_ID, result.getResourceId());
        assertInstanceOf(DiagnosticsCollectionEvent.class, result);

        DiagnosticsCollectionEvent resultEvent = (DiagnosticsCollectionEvent) result;
        assertEquals(RESOURCE_CRN, resultEvent.getResourceCrn());
        assertEquals(parameters, resultEvent.getParameters());
        assertEquals(Set.of("host1"), resultEvent.getHosts());
        assertEquals(Set.of("hostgroup1"), resultEvent.getHostGroups());
        assertEquals(Set.of("excludehost1"), resultEvent.getExcludeHosts());
    }

    @Test
    public void testExecuteOperationWithException() throws Exception {
        // GIVEN
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        DiagnosticParameters parameters = new DiagnosticParameters();

        doThrow(new RuntimeException("Network check failed")).when(diagnosticsFlowService).nodeStatusNetworkReport(any(Stack.class));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);

        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector(),
                STACK_ID, RESOURCE_CRN, parameters, Set.of(), Set.of(), Set.of());

        // WHEN & THEN
        Exception exception = assertThrows(RuntimeException.class,
                () -> underTest.executeOperation(new HandlerEvent<>(new Event<>(event))));
        assertEquals("Network check failed", exception.getMessage());

        verify(diagnosticsFlowService, times(1)).nodeStatusNetworkReport(stack);
        verify(stackService, times(1)).getByIdWithListsInTransaction(STACK_ID);
    }

    @Test
    public void testDoAcceptSuccess() {
        // GIVEN
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        DiagnosticParameters parameters = new DiagnosticParameters();

        doNothing().when(diagnosticsFlowService).nodeStatusNetworkReport(any(Stack.class));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);

        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector(),
                STACK_ID, RESOURCE_CRN, parameters, Set.of(), Set.of(), Set.of());

        // WHEN
        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        // THEN
        verify(diagnosticsFlowService, times(1)).nodeStatusNetworkReport(stack);
        assertEquals(START_DIAGNOSTICS_INIT_EVENT.selector(), result.selector());
    }

    @Test
    public void testDoAcceptOnError() {
        // GIVEN
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        DiagnosticParameters parameters = new DiagnosticParameters();

        doThrow(new IllegalArgumentException("Network error")).when(diagnosticsFlowService).nodeStatusNetworkReport(any(Stack.class));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);

        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector(),
                STACK_ID, RESOURCE_CRN, parameters, Set.of(), Set.of(), Set.of());

        // WHEN & THEN
        DiagnosticsFlowException result = assertThrows(DiagnosticsFlowException.class,
                () -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))));

        assertTrue(result.getMessage().contains("Error during diagnostics operation: Pre-flight check"));
        assertTrue(result.getMessage().contains("Network error"));
        verify(diagnosticsFlowService, times(1)).nodeStatusNetworkReport(stack);
    }

    @Test
    public void testGetFailureType() {
        assertEquals(UsageProto.CDPVMDiagnosticsFailureType.Value.UNSET, underTest.getFailureType());
    }

    @Test
    public void testGetOperationName() {
        assertEquals("Pre-flight check", underTest.getOperationName());
    }

    @Test
    public void testSelector() {
        assertEquals(PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector(), underTest.selector());
    }

    @Test
    public void testDefaultFailureEvent() {
        // GIVEN
        DiagnosticParameters parameters = new DiagnosticParameters();
        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector(),
                STACK_ID, RESOURCE_CRN, parameters, Set.of(), Set.of(), Set.of());
        Exception exception = new IllegalArgumentException("Test exception");

        // WHEN
        Selectable result = underTest.defaultFailureEvent(STACK_ID, exception, new Event<>(event));

        // THEN
        assertEquals(FAILED_DIAGNOSTICS_COLLECTION_EVENT.selector(), result.selector());
        assertEquals(STACK_ID, result.getResourceId());
    }
}
