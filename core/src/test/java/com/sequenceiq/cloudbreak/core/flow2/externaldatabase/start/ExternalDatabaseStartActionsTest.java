package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start;

import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartEvent.EXTERNAL_DATABASE_STARTED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartEvent.EXTERNAL_DATABASE_START_FAILED_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.state.State;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.StartExternalDatabaseFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.StartExternalDatabaseResult;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.MessageFactory;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class ExternalDatabaseStartActionsTest {

    private static final String FLOW_ID = "flowId";

    private static final String FLOW_TRIGGER_USER_CRN = "flowTriggerUserCrn";

    private static final String ACTION_PAYLOAD_SELECTOR = "selector";

    private static final Long STACK_ID = 1234L;

    private static final String MESSAGE = "Houston, we have a problem.";

    private static final String STACK_NAME = "stackName";

    private static final String STACK_CRN = "stackCrn";

    private static final Stack STACK = new Stack();

    @Mock
    private StackUpdaterService stackUpdaterService;

    @Mock
    private StateContext stateContext;

    @Mock
    private ExtendedState extendedState;

    @Mock
    private StateMachine stateMachine;

    @Mock
    private State state;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private FlowEvent failureEvent;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Event<Object> event;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CloudbreakMetricService metricService;

    @Captor
    private ArgumentCaptor<String> selectorArgumentCaptor;

    @Captor
    private ArgumentCaptor<Event<?>> eventArgumentCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> headersArgumentCaptor;

    @Captor
    private ArgumentCaptor<Object> payloadArgumentCaptor;

    @Captor
    private ArgumentCaptor<Exception> exceptionCaptor;

    @Mock
    private Flow flow;

    @Mock
    private FlowEvent flowEvent;

    @InjectMocks
    private ExternalDatabaseStartActions underTest;

    @BeforeEach
    void setup() {
        STACK.setId(STACK_ID);
        FlowParameters flowParameters = new FlowParameters(FLOW_ID, FLOW_TRIGGER_USER_CRN);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_PARAMETERS.name())).thenReturn(flowParameters);
        when(stateContext.getExtendedState()).thenReturn(extendedState);
        when(extendedState.getVariables()).thenReturn(new HashMap<>());
        when(stateContext.getStateMachine()).thenReturn(stateMachine);
        when(stateMachine.getState()).thenReturn(state);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);
        when(stackDtoService.getStackViewById(any())).thenReturn(STACK);
        Workspace workspace = new Workspace();
        workspace.setTenant(new Tenant());
        STACK.setWorkspace(workspace);
    }

    @Test
    void externalDatabaseStartRequestAction() {
        StackEvent stackEventPayload = new StackEvent(ACTION_PAYLOAD_SELECTOR, STACK_ID);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(stackEventPayload);
        Action<?, ?> action = configureAction(underTest::externalDatabaseStartRequestAction);
        action.execute(stateContext);
        verifyNoMoreInteractions(stackUpdaterService);
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo("StartExternalDatabaseRequest");
    }

    @Test
    void externalDatabaseStartFinishedAction() {
        StartExternalDatabaseResult startExternalDatabaseResultPayload =
                new StartExternalDatabaseResult(STACK_ID, EXTERNAL_DATABASE_STARTED_EVENT.event(), STACK_NAME, STACK_CRN);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(startExternalDatabaseResultPayload);
        Action<?, ?> action = configureAction(underTest::externalDatabaseStartFinishedAction);
        action.execute(stateContext);

        verifyNoMoreInteractions(stackUpdaterService);
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());
        verify(metricService).incrementMetricCounter(MetricType.EXTERNAL_DATABASE_START_SUCCESSFUL, STACK);
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo("EXTERNAL_DATABASE_START_FINALIZED_EVENT");
        Object capturedPayload = payloadArgumentCaptor.getValue();
        assertThat(capturedPayload).isInstanceOf(StackEvent.class);
        StackEvent stackEvent = (StackEvent) capturedPayload;
        assertThat(stackEvent.getResourceId()).isEqualTo(STACK_ID);
    }

    @Test
    void externalDatabaseCreationFailureAction() {
        RuntimeException expectedException = new RuntimeException(MESSAGE);
        StartExternalDatabaseFailed startExternalDatabaseFailedPayload =
                new StartExternalDatabaseFailed(STACK_ID, EXTERNAL_DATABASE_START_FAILED_EVENT.event(),
                        STACK_NAME, null, expectedException);

        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(startExternalDatabaseFailedPayload);
        Action<?, ?> action = configureAction(underTest::externalDatabaseStartFailureAction);
        action.execute(stateContext);

        verify(stackUpdaterService).updateStatus(STACK_ID, DetailedStackStatus.EXTERNAL_DATABASE_START_FAILED,
                ResourceEvent.CLUSTER_EXTERNAL_DATABASE_START_FAILED, MESSAGE);
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());
        verify(metricService).incrementMetricCounter(MetricType.EXTERNAL_DATABASE_START_FAILED, STACK);
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo("EXTERNAL_DATABASE_START_FAILURE_HANDLED_EVENT");
        Object capturedPayload = payloadArgumentCaptor.getValue();
        assertThat(capturedPayload).isInstanceOf(StackEvent.class);
        StackEvent stackEvent = (StackEvent) capturedPayload;
        assertThat(stackEvent.getResourceId()).isEqualTo(STACK_ID);
    }

    private Action<?, ?> configureAction(Supplier<Action<?, ?>> actionSupplier) {
        Action<?, ?> action = actionSupplier.get();
        assertThat(action).isNotNull();
        setActionPrivateFields(action);
        AbstractAction abstractAction = (AbstractAction) action;
        abstractAction.setFailureEvent(failureEvent);
        return action;
    }

    private void setActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, stackDtoService, StackDtoService.class);
        ReflectionTestUtils.setField(action, null, metricService, MetricService.class);
    }

}
