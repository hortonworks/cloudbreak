package com.sequenceiq.environment.environment.flow.deletion;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_COMPUTE_CLUSTERS_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_DATAHUB_CLUSTERS_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_DATALAKE_CLUSTERS_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_EXPERIENCE_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.events.EventSenderService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.MessageFactory.HEADERS;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class EnvClustersDeleteActionsTest {

    private static final String FLOW_ID = "flowId";

    private static final String FLOW_TRIGGER_USER_CRN = "flowTriggerUserCrn";

    private static final Long ENVIRONMENT_ID = 1234L;

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String ACTION_PAYLOAD_SELECTOR = "selector";

    private static final String MESSAGE = "Error Message";

    private static final String FAILURE_EVENT = "failureEvent";

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private StateContext stateContext;

    @Mock
    private ExtendedState extendedState;

    @Mock
    private StateMachine stateMachine;

    @Mock
    private State state;

    @Mock
    private Environment environment;

    @Mock
    private Environment savedEnvironment;

    @Mock
    private EnvironmentDto environmentDto;

    @Mock
    private SimpleEnvironmentResponse simpleEnvironmentResponse;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private FlowEvent failureEvent;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Event<Object> event;

    @Mock
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @InjectMocks
    private EnvClustersDeleteActions underTest;

    @Captor
    private ArgumentCaptor<String> selectorArgumentCaptor;

    @Captor
    private ArgumentCaptor<Event<?>> eventArgumentCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> headersArgumentCaptor;

    @Captor
    private ArgumentCaptor<Object> payloadArgumentCaptor;

    private EnvDeleteEvent actionPayload;

    @Mock
    private FlowEvent flowEvent;

    @BeforeEach
    void setUp() {
        FlowParameters flowParameters = new FlowParameters(FLOW_ID, FLOW_TRIGGER_USER_CRN);
        actionPayload = new EnvDeleteEvent(ACTION_PAYLOAD_SELECTOR, ENVIRONMENT_ID, ENVIRONMENT_NAME, ENVIRONMENT_CRN, true);

        when(stateContext.getMessageHeader(HEADERS.FLOW_PARAMETERS.name())).thenReturn(flowParameters);
        when(stateContext.getMessageHeader(HEADERS.DATA.name())).thenReturn(actionPayload);
        when(stateContext.getExtendedState()).thenReturn(extendedState);
        when(stateContext.getStateMachine()).thenReturn(stateMachine);
        when(stateMachine.getState()).thenReturn(state);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);
    }

    @Test
    void datahubClustersDeleteActionFailure() {
        Action<?, ?> action = configureAction(() -> underTest.datahubClustersDeleteAction());

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

    @Test
    void datahubClustersDeleteActionNoEnvironment() {
        Action<?, ?> action = configureAction(() -> underTest.datahubClustersDeleteAction());

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.empty());

        action.execute(stateContext);

        verify(environmentService, never()).save(any(Environment.class));
        verify(environmentService, never()).getEnvironmentDto(any(Environment.class));
        verify(environment, never()).setStatus(any());
        verify(eventSenderService, never()).sendEventAndNotification(any(EnvironmentDto.class), anyString(), any(ResourceEvent.class));
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());

        verifyDeleteActionSuccessEvent(DELETE_DATAHUB_CLUSTERS_EVENT);
    }

    @Test
    void datahubClustersDeleteAction() {
        Action<?, ?> action = configureAction(() -> underTest.datahubClustersDeleteAction());

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        when(environmentService.save(environment)).thenReturn(savedEnvironment);
        when(environmentService.getEnvironmentDto(savedEnvironment)).thenReturn(environmentDto);

        action.execute(stateContext);

        verify(environment).setStatus(EnvironmentStatus.DATAHUB_CLUSTERS_DELETE_IN_PROGRESS);
        verify(eventSenderService).sendEventAndNotification(environmentDto, FLOW_TRIGGER_USER_CRN, ResourceEvent.ENVIRONMENT_DATAHUB_CLUSTERS_DELETION_STARTED);
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());

        verifyDeleteActionSuccessEvent(DELETE_DATAHUB_CLUSTERS_EVENT);
    }

    @Test
    void datalakeClustersDeleteActionFailure() {
        Action<?, ?> action = configureAction(() -> underTest.datalakeClustersDeleteAction());

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

    @Test
    void datalakeClustersDeleteActionNoEnvironment() {
        Action<?, ?> action = configureAction(() -> underTest.datalakeClustersDeleteAction());

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.empty());

        action.execute(stateContext);

        verify(environmentService, never()).save(any(Environment.class));
        verify(environmentService, never()).getEnvironmentDto(any(Environment.class));
        verify(environment, never()).setStatus(any());
        verify(eventSenderService, never()).sendEventAndNotification(any(EnvironmentDto.class), anyString(), any(ResourceEvent.class));
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());

        verifyDeleteActionSuccessEvent(DELETE_DATALAKE_CLUSTERS_EVENT);
    }

    @Test
    void datalakeClustersDeleteActionWhenNotHybrid() {
        Action<?, ?> action = configureAction(() -> underTest.datalakeClustersDeleteAction());

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        when(environmentService.save(environment)).thenReturn(savedEnvironment);
        when(environmentService.isHybridEnvironment(any())).thenReturn(false);
        when(environmentService.getEnvironmentDto(savedEnvironment)).thenReturn(environmentDto);

        action.execute(stateContext);

        verify(environment).setStatus(EnvironmentStatus.DATALAKE_CLUSTERS_DELETE_IN_PROGRESS);
        verify(eventSenderService).sendEventAndNotification(environmentDto, FLOW_TRIGGER_USER_CRN, ResourceEvent.ENVIRONMENT_DATALAKE_CLUSTERS_DELETION_STARTED);
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());

        verifyDeleteActionSuccessEvent(DELETE_DATALAKE_CLUSTERS_EVENT);
    }

    @Test
    void datalakeClustersDeleteActionWhenHybrid() {
        Action<?, ?> action = configureAction(() -> underTest.datalakeClustersDeleteAction());

        when(environmentService.isHybridEnvironment(any())).thenReturn(true);

        action.execute(stateContext);

        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());

        verifyDeleteActionSuccessEvent(DELETE_DATALAKE_CLUSTERS_EVENT);
    }

    @Test
    void experienceDeleteActionFailure() {
        Action<?, ?> action = configureAction(() -> underTest.experienceDeleteAction());


        when(environmentStatusUpdateService.updateEnvironmentStatusAndNotify(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException(MESSAGE));
        when(failureEvent.event()).thenReturn(FAILURE_EVENT);

        action.execute(stateContext);

        verify(environmentService, never()).save(any(Environment.class));
        verify(environmentService, never()).getEnvironmentDto(any(Environment.class));
        verify(eventSenderService, never()).sendEventAndNotification(any(EnvironmentDto.class), anyString(), any(ResourceEvent.class));
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());

        verifyFailureEvent();
    }

    @Test
    void experienceDeleteAction() {
        Action<?, ?> action = configureAction(() -> underTest.experienceDeleteAction());

        when(environmentDto.getId()).thenReturn(ENVIRONMENT_ID);
        when(environmentDto.getName()).thenReturn(ENVIRONMENT_NAME);
        when(environmentDto.getResourceCrn()).thenReturn(ENVIRONMENT_CRN);
        when(environmentStatusUpdateService.updateEnvironmentStatusAndNotify(any(), any(), any(), any(), any()))
                .thenReturn(environmentDto);

        action.execute(stateContext);

        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());

        verifyDeleteActionSuccessEvent(DELETE_EXPERIENCE_EVENT);
    }

    @Test
    void computeClusterDeleteActionFailure() {
        Action<?, ?> action = configureAction(() -> underTest.externalizedComputeClustersDeleteAction());

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenThrow(new IllegalStateException(MESSAGE));
        when(failureEvent.event()).thenReturn(FAILURE_EVENT);

        action.execute(stateContext);

        verify(environmentService, never()).save(any(Environment.class));
        verify(environmentService, never()).getEnvironmentDto(any(Environment.class));
        verify(eventSenderService, never()).sendEventAndNotification(any(EnvironmentDto.class), anyString(), any(ResourceEvent.class));
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());

        verifyFailureEvent();
    }

    @Test
    void computeClusterDeleteAction() {
        Action<?, ?> action = configureAction(() -> underTest.externalizedComputeClustersDeleteAction());

        when(environmentService.save(any())).thenReturn(savedEnvironment);
        when(environmentService.getEnvironmentDto(savedEnvironment)).thenReturn(environmentDto);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));

        action.execute(stateContext);

        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());
        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentService).save(environmentArgumentCaptor.capture());
        assertEquals(EnvironmentStatus.COMPUTE_CLUSTERS_DELETE_IN_PROGRESS, environmentArgumentCaptor.getValue().getStatus());
        verify(eventSenderService, times(1)).sendEventAndNotification(any(EnvironmentDto.class), anyString(), any(ResourceEvent.class));

        verifyDeleteActionSuccessEvent(DELETE_COMPUTE_CLUSTERS_EVENT);
    }

    private void setActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }

    private Action<?, ?> configureAction(Supplier<Action<?, ?>> actionSupplier) {
        Action<?, ?> action = actionSupplier.get();
        assertThat(action).isNotNull();
        setActionPrivateFields(action);
        AbstractAction abstractAction = (AbstractAction) action;
        abstractAction.setFailureEvent(failureEvent);
        return action;
    }

    private void verifyFailureEvent() {
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(FAILURE_EVENT);

        verifyEventFactoryAndHeaders();

        assertThat(payloadArgumentCaptor.getValue()).isSameAs(actionPayload);
    }

    private void verifyDeleteActionSuccessEvent(EnvDeleteHandlerSelectors eventSelector) {
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(eventSelector.selector());

        verifyEventFactoryAndHeaders();

        Object payload = payloadArgumentCaptor.getValue();
        assertThat(payload).isInstanceOf(EnvironmentDeletionDto.class);

        EnvironmentDeletionDto environmentDto = (EnvironmentDeletionDto) payload;
        assertThat(environmentDto.getEnvironmentDto().getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(environmentDto.getEnvironmentDto().getName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(environmentDto.getEnvironmentDto().getId()).isEqualTo(ENVIRONMENT_ID);
    }

    private void verifyEventFactoryAndHeaders() {
        assertThat(eventArgumentCaptor.getValue()).isSameAs(event);

        Map<String, Object> headers = headersArgumentCaptor.getValue();
        assertThat(headers).isNotNull();
        assertThat(headers.get(FlowConstants.FLOW_ID)).isEqualTo(FLOW_ID);
        assertThat(headers.get(FlowConstants.FLOW_TRIGGER_USERCRN)).isEqualTo(FLOW_TRIGGER_USER_CRN);
    }
}
