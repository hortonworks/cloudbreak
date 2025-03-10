package com.sequenceiq.environment.environment.flow.creation;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_PUBLICKEY_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.INITIALIZE_ENVIRONMENT_RESOURCE_ENCRYPTION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FAILED_ENV_CREATION_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.scheduled.sync.EnvironmentJobService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.events.EventSenderService;
import com.sequenceiq.environment.exception.ExternalizedComputeOperationFailedException;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.MessageFactory;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class EnvCreationActionsTest {
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
    private EventSenderService eventSenderService;

    @Mock
    private EnvironmentMetricService metricService;

    @Mock
    private EnvironmentJobService environmentJobService;

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

    @InjectMocks
    private EnvCreationActions underTest;

    private EnvCreationEvent actionPayload;

    @Captor
    private ArgumentCaptor<String> selectorArgumentCaptor;

    @Captor
    private ArgumentCaptor<Event<?>> eventArgumentCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> headersArgumentCaptor;

    @Captor
    private ArgumentCaptor<Object> payloadArgumentCaptor;

    @Mock
    private FlowEvent flowEvent;

    @BeforeEach
    void setUp() {
        FlowParameters flowParameters = new FlowParameters(FLOW_ID, FLOW_TRIGGER_USER_CRN);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_PARAMETERS.name())).thenReturn(flowParameters);
        actionPayload = new EnvCreationEvent(ACTION_PAYLOAD_SELECTOR, ENVIRONMENT_ID, ENVIRONMENT_NAME, ENVIRONMENT_CRN);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(actionPayload);
        when(stateContext.getExtendedState()).thenReturn(extendedState);
        when(extendedState.getVariables()).thenReturn(new HashMap<>());
        when(stateContext.getStateMachine()).thenReturn(stateMachine);
        when(stateMachine.getState()).thenReturn(state);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);
    }

    @Test
    void publicKeyCreationActionTestFailure() {
        testFailure(underTest::publickeyCreationAction);
    }

    @Test
    void publicKeyCreationActionTestNoEnvironment() {
        testNoEnvironment(underTest::publickeyCreationAction, FAILED_ENV_CREATION_EVENT.selector(), ResourceEvent.ENVIRONMENT_PUBLICKEY_CREATION_FAILED);
    }

    @Test
    void publicKeyCreationActionTestHappyPath() {
        testCreationActionHappyPath(underTest::publickeyCreationAction, CREATE_PUBLICKEY_EVENT.selector(),
                EnvironmentStatus.PUBLICKEY_CREATE_IN_PROGRESS, ResourceEvent.ENVIRONMENT_PUBLICKEY_CREATION_STARTED);
    }

    @Test
    void resourceEncryptionInitializationActionTestFailure() {
        testFailure(underTest::resourceEncryptionInitializationAction);
    }

    @Test
    void resourceEncryptionInitializationActionTestNoEnvironment() {
        testNoEnvironment(underTest::resourceEncryptionInitializationAction, FAILED_ENV_CREATION_EVENT.selector(),
                ResourceEvent.ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_FAILED);
    }

    @Test
    void resourceEncryptionInitializationActionTestHappyPath() {
        testCreationActionHappyPath(underTest::resourceEncryptionInitializationAction, INITIALIZE_ENVIRONMENT_RESOURCE_ENCRYPTION_EVENT.selector(),
                EnvironmentStatus.ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_IN_PROGRESS,
                ResourceEvent.ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_STARTED);
    }

    @Test
    void testFailedActionIfComputeClusterFailed() {
        Environment environment = mock(Environment.class);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        EnvCreationFailureEvent envCreationFailureEvent = new EnvCreationFailureEvent(ENVIRONMENT_ID, ENVIRONMENT_NAME,
                new Exception("Compute cluster failed", new ExternalizedComputeOperationFailedException("Liftie cluster failed")), ENVIRONMENT_CRN);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(envCreationFailureEvent);
        Action<?, ?> action = configureAction(() -> underTest.failedAction());
        action.execute(stateContext);
        verify(environment).setStatus(EnvironmentStatus.AVAILABLE);
        verify(environment).setStatusReason("Liftie cluster failed");
    }

    private void testFailure(Supplier<Action<?, ?>> creationAction) {
        Action<?, ?> action = configureAction(creationAction);

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenThrow(new UnsupportedOperationException(MESSAGE));
        when(failureEvent.event()).thenReturn(FAILURE_EVENT);

        action.execute(stateContext);

        verify(environmentService, never()).save(any(Environment.class));
        verify(environmentService, never()).getEnvironmentDto(any(Environment.class));
        verify(eventSenderService, never()).sendEventAndNotification(any(EnvironmentDto.class), anyString(), any(ResourceEvent.class));
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());

        verifyFailureEvent();
    }

    private void testNoEnvironment(Supplier<Action<?, ?>> creationAction, String selector, ResourceEvent eventFailed) {
        Action<?, ?> action = configureAction(creationAction);

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.empty());

        action.execute(stateContext);

        verify(environmentService, never()).save(any(Environment.class));
        verify(environmentService, never()).getEnvironmentDto(any(Environment.class));
        verify(eventSenderService, never()).sendEventAndNotification(any(EnvironmentDto.class), anyString(), any(ResourceEvent.class));
        verify(eventSenderService).sendEventAndNotificationForMissingEnv(actionPayload, eventFailed, FLOW_TRIGGER_USER_CRN);
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());

        verifyCreationActionFailureEvent(selector);
    }

    private void testCreationActionHappyPath(Supplier<Action<?, ?>> creationAction,
            String selector,
            EnvironmentStatus environmentStatus,
            ResourceEvent eventStarted) {

        Action<?, ?> action = configureAction(creationAction);

        Environment environment = mock(Environment.class);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        Environment savedEnvironment = mock(Environment.class);
        when(environmentService.save(environment)).thenReturn(savedEnvironment);
        EnvironmentDto environmentDto = mock(EnvironmentDto.class);
        when(environmentDto.getResourceCrn()).thenReturn(ENVIRONMENT_CRN);
        when(environmentDto.getName()).thenReturn(ENVIRONMENT_NAME);
        when(environmentDto.getId()).thenReturn(ENVIRONMENT_ID);
        when(environmentService.getEnvironmentDto(savedEnvironment)).thenReturn(environmentDto);

        action.execute(stateContext);

        verify(environment).setStatus(environmentStatus);
        verify(eventSenderService).sendEventAndNotification(environmentDto, FLOW_TRIGGER_USER_CRN, eventStarted);
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());

        verifyCreationActionSuccessEvent(selector);
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

    private void verifyCreationActionSuccessEvent(String selector) {
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(selector);

        verifyEventFactoryAndHeaders();

        Object payload = payloadArgumentCaptor.getValue();
        assertThat(payload).isInstanceOf(EnvironmentDto.class);

        EnvironmentDto environmentDto = (EnvironmentDto) payload;
        assertThat(environmentDto.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(environmentDto.getName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(environmentDto.getId()).isEqualTo(ENVIRONMENT_ID);
    }

    private void verifyCreationActionFailureEvent(String selector) {
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(selector);

        verifyEventFactoryAndHeaders();

        Object payload = payloadArgumentCaptor.getValue();
        assertThat(payload).isInstanceOf(EnvCreationFailureEvent.class);

        EnvCreationFailureEvent failureEvent = (EnvCreationFailureEvent) payload;
        assertThat(failureEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(failureEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(failureEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
    }

    private void verifyEventFactoryAndHeaders() {
        assertThat(eventArgumentCaptor.getValue()).isSameAs(event);

        Map<String, Object> headers = headersArgumentCaptor.getValue();
        assertThat(headers).isNotNull();
        assertThat(headers.get(FlowConstants.FLOW_ID)).isEqualTo(FLOW_ID);
        assertThat(headers.get(FlowConstants.FLOW_TRIGGER_USERCRN)).isEqualTo(FLOW_TRIGGER_USER_CRN);
    }

}
