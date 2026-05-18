package com.sequenceiq.freeipa.flow.freeipa.diagnostics;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_DIAGNOSTICS_COLLECTION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_DIAGNOSTICS_COLLECTION_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_DIAGNOSTICS_COLLECTION_STARTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.telemetry.diagnostics.DiagnosticsOperationsService;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors;

@ExtendWith(MockitoExtension.class)
class DiagnosticsCollectionActionsTest {

    @InjectMocks
    private DiagnosticsCollectionActions underTest;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private DiagnosticsOperationsService diagnosticsOperationsService;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    private CommonContext context;

    @BeforeEach
    void setUp() {
        context = new CommonContext(new FlowParameters("flow", "user-crn"));
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), "ok")).when(reactorEventFactory).createEvent(any(), any());
    }

    @Test
    void diagnosticsSaltValidationSendsStartedNotification() throws Exception {
        DiagnosticsCollectionEvent payload = new DiagnosticsCollectionEvent(
                DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_COLLECTION_EVENT.selector(), 1L,
                "crn:cdp:freeipa:test", new DiagnosticParameters());
        Map<Object, Object> variables = new HashMap<>();

        AbstractDiagnosticsCollectionActions<DiagnosticsCollectionEvent> action =
                (AbstractDiagnosticsCollectionActions<DiagnosticsCollectionEvent>) underTest.diagnosticsSaltValidateAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotificationWithoutStack(payload, FREEIPA_DIAGNOSTICS_COLLECTION_STARTED, "user-crn");
    }

    @Test
    void diagnosticsFinishedSendsFinishedNotification() throws Exception {
        DiagnosticsCollectionEvent payload = new DiagnosticsCollectionEvent(
                DiagnosticsCollectionStateSelectors.FINALIZE_DIAGNOSTICS_COLLECTION_EVENT.selector(), 1L, "crn:cdp:freeipa:test",
                new DiagnosticParameters());
        Map<Object, Object> variables = new HashMap<>();

        AbstractDiagnosticsCollectionActions<DiagnosticsCollectionEvent> action =
                (AbstractDiagnosticsCollectionActions<DiagnosticsCollectionEvent>) underTest.diagnosticsCollectionFinishedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotificationWithoutStack(payload, FREEIPA_DIAGNOSTICS_COLLECTION_FINISHED, "user-crn");
    }

    @Test
    void diagnosticsFailureSendsFailedNotificationWithErrorReason() throws Exception {
        DiagnosticsCollectionFailureEvent payload = new DiagnosticsCollectionFailureEvent(
                1L, new Exception("boom"), "crn:cdp:freeipa:test", new DiagnosticParameters(), "UNSET");
        Map<Object, Object> variables = new HashMap<>();

        AbstractDiagnosticsCollectionActions<DiagnosticsCollectionFailureEvent> action =
                (AbstractDiagnosticsCollectionActions<DiagnosticsCollectionFailureEvent>) underTest.failedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotificationWithoutStack(payload, FREEIPA_DIAGNOSTICS_COLLECTION_FAILED,
                "user-crn", List.of("boom"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}



