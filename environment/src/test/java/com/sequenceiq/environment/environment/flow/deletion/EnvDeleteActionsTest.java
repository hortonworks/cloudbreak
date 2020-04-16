package com.sequenceiq.environment.environment.flow.deletion;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_IDBROKER_MAPPINGS_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_PUBLICKEY_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentResponseConverter;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.MessageFactory;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.notification.NotificationService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class EnvDeleteActionsTest {

    private static final String FLOW_ID = "flowId";

    private static final String FLOW_TRIGGER_USER_CRN = "flowTriggerUserCrn";

    private static final String ACTION_PAYLOAD_SELECTOR = "selector";

    private static final Long ENVIRONMENT_ID = 1234L;

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String MESSAGE = "Houston, we have a problem.";

    private static final String FAILURE_EVENT = "failureEvent";

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EnvironmentResponseConverter environmentResponseConverter;

    @Mock
    private StateContext context;

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
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @InjectMocks
    private EnvDeleteActions underTest;

    private EnvDeleteEvent actionPayload;

    @Captor
    private ArgumentCaptor<String> selectorArgumentCaptor;

    @Captor
    private ArgumentCaptor<Event<?>> eventArgumentCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> headersArgumentCaptor;

    @Captor
    private ArgumentCaptor<Object> payloadArgumentCaptor;

    @BeforeEach
    void setUp() {
        FlowParameters flowParameters = new FlowParameters(FLOW_ID, FLOW_TRIGGER_USER_CRN);
        when(context.getMessageHeader(MessageFactory.HEADERS.FLOW_PARAMETERS.name())).thenReturn(flowParameters);
        actionPayload = new EnvDeleteEvent(ACTION_PAYLOAD_SELECTOR, ENVIRONMENT_ID, ENVIRONMENT_NAME, ENVIRONMENT_CRN);
        when(context.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(actionPayload);
        when(context.getExtendedState()).thenReturn(extendedState);
        when(extendedState.getVariables()).thenReturn(new HashMap<>());
        when(context.getStateMachine()).thenReturn(stateMachine);
        when(stateMachine.getState()).thenReturn(state);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);
    }

    @Test
    void idbmmsDeleteActionTestHappyPath() {
        testDeleteActionHappyPath(underTest::idbmmsDeleteAction, DELETE_IDBROKER_MAPPINGS_EVENT.selector(),
                EnvironmentStatus.IDBROKER_MAPPINGS_DELETE_IN_PROGRESS, ResourceEvent.ENVIRONMENT_IDBROKER_MAPPINGS_DELETION_STARTED);
    }

    @Test
    void idbmmsDeleteActionTestNoEnvironment() {
        testNoEnvironment(underTest::idbmmsDeleteAction, DELETE_IDBROKER_MAPPINGS_EVENT.selector());
    }

    @Test
    void publicKeyDeleteActionTestHappyPath() {
        testDeleteActionHappyPath(underTest::publickeyDeleteAction, DELETE_PUBLICKEY_EVENT.selector(),
                EnvironmentStatus.PUBLICKEY_DELETE_IN_PROGRESS, ResourceEvent.ENVIRONMENT_PUBLICKEY_DELETION_STARTED);
    }

    @Test
    void publicKeyDeleteActionTestNoEnvironment() {
        testNoEnvironment(underTest::publickeyDeleteAction, DELETE_PUBLICKEY_EVENT.selector());
    }

    private void testNoEnvironment(Supplier<Action<?, ?>> deleteAction, String selector) {
        Action<?, ?> action = configureAction(deleteAction);

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.empty());

        action.execute(context);

        verify(environmentService, never()).save(any(Environment.class));
        verify(environmentService, never()).getEnvironmentDto(any(Environment.class));
        verify(environmentResponseConverter, never()).dtoToSimpleResponse(any(EnvironmentDto.class));
        verify(notificationService, never()).send(any(ResourceEvent.class), any(SimpleEnvironmentResponse.class), anyString());
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
    }

    private void testDeleteActionHappyPath(Supplier<Action<?, ?>> deleteAction,
            String selector,
            EnvironmentStatus environmentStatus,
            ResourceEvent eventStarted) {

        Action<?, ?> action = configureAction(deleteAction);

        EnvironmentDto environmentDto = mock(EnvironmentDto.class);
        when(environmentStatusUpdateService.updateEnvironmentStatusAndNotify(any(), any(), any(), any(), any())).thenReturn(environmentDto);

        action.execute(context);

        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());
        verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(any(), any(), eq(environmentStatus), eq(eventStarted), any());
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
    }

    private void verifyFailureEvent() {
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(FAILURE_EVENT);

        verifyEventFactoryAndHeaders();

        assertThat(payloadArgumentCaptor.getValue()).isSameAs(actionPayload);
    }

    private void verifyEventFactoryAndHeaders() {
        assertThat(eventArgumentCaptor.getValue()).isSameAs(event);

        Map<String, Object> headers = headersArgumentCaptor.getValue();
        assertThat(headers).isNotNull();
        assertThat(headers.get(FlowConstants.FLOW_ID)).isEqualTo(FLOW_ID);
        assertThat(headers.get(FlowConstants.FLOW_TRIGGER_USERCRN)).isEqualTo(FLOW_TRIGGER_USER_CRN);
    }

}
