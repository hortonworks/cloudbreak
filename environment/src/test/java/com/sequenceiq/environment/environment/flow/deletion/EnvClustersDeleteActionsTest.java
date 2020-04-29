package com.sequenceiq.environment.environment.flow.deletion;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_DATAHUB_CLUSTERS_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_DATALAKE_CLUSTERS_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.never;
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
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentResponseConverter;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.MessageFactory.HEADERS;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.notification.NotificationService;

import reactor.bus.Event;
import reactor.bus.EventBus;

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

    @BeforeEach
    void setUp() {
        FlowParameters flowParameters = new FlowParameters(FLOW_ID, FLOW_TRIGGER_USER_CRN);
        actionPayload = new EnvDeleteEvent(ACTION_PAYLOAD_SELECTOR, ENVIRONMENT_ID, ENVIRONMENT_NAME, ENVIRONMENT_CRN);

        when(context.getMessageHeader(HEADERS.FLOW_PARAMETERS.name())).thenReturn(flowParameters);
        when(context.getMessageHeader(HEADERS.DATA.name())).thenReturn(actionPayload);
        when(context.getExtendedState()).thenReturn(extendedState);
        when(context.getStateMachine()).thenReturn(stateMachine);
        when(stateMachine.getState()).thenReturn(state);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);
    }

    @Test
    void datahubClustersDeleteActionFailure() {
        Action<?, ?> action = configureAction(() -> underTest.datahubClustersDeleteAction());

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenThrow(new UnsupportedOperationException(MESSAGE));
        when(failureEvent.event()).thenReturn(FAILURE_EVENT);

        action.execute(context);

        verify(environmentService, never()).save(any(Environment.class));
        verify(environmentService, never()).getEnvironmentDto(any(Environment.class));
        verify(environmentResponseConverter, never()).dtoToSimpleResponse(any(EnvironmentDto.class));
        verify(notificationService, never()).send(any(ResourceEvent.class), any(SimpleEnvironmentResponse.class), anyString());
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());

        verifyFailureEvent();
    }

    @Test
    void datahubClustersDeleteActionNoEnvironment() {
        Action<?, ?> action = configureAction(() -> underTest.datahubClustersDeleteAction());

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.empty());

        action.execute(context);

        verify(environmentService, never()).save(any(Environment.class));
        verify(environmentService, never()).getEnvironmentDto(any(Environment.class));
        verify(environmentService, never()).getEnvironmentDto(any(Environment.class));
        verify(environmentResponseConverter, never()).dtoToSimpleResponse(any(EnvironmentDto.class));
        verify(environment, never()).setStatus(any());
        verify(notificationService, never()).send(any(ResourceEvent.class), any(SimpleEnvironmentResponse.class), any());
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
        when(environmentResponseConverter.dtoToSimpleResponse(environmentDto)).thenReturn(simpleEnvironmentResponse);

        action.execute(context);

        verify(environment).setStatus(EnvironmentStatus.DATAHUB_CLUSTERS_DELETE_IN_PROGRESS);
        verify(notificationService).send(ResourceEvent.ENVIRONMENT_DATAHUB_CLUSTERS_DELETION_STARTED, simpleEnvironmentResponse, FLOW_TRIGGER_USER_CRN);
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());

        verifyDeleteActionSuccessEvent(DELETE_DATAHUB_CLUSTERS_EVENT);
    }

    @Test
    void datalakeClustersDeleteActionFailure() {
        Action<?, ?> action = configureAction(() -> underTest.datalakeClustersDeleteAction());

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenThrow(new UnsupportedOperationException(MESSAGE));
        when(failureEvent.event()).thenReturn(FAILURE_EVENT);

        action.execute(context);

        verify(environmentService, never()).save(any(Environment.class));
        verify(environmentService, never()).getEnvironmentDto(any(Environment.class));
        verify(environmentResponseConverter, never()).dtoToSimpleResponse(any(EnvironmentDto.class));
        verify(notificationService, never()).send(any(ResourceEvent.class), any(SimpleEnvironmentResponse.class), anyString());
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());

        verifyFailureEvent();
    }

    @Test
    void datalakeClustersDeleteActionNoEnvironment() {
        Action<?, ?> action = configureAction(() -> underTest.datalakeClustersDeleteAction());

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.empty());

        action.execute(context);

        verify(environmentService, never()).save(any(Environment.class));
        verify(environmentService, never()).getEnvironmentDto(any(Environment.class));
        verify(environmentService, never()).getEnvironmentDto(any(Environment.class));
        verify(environmentResponseConverter, never()).dtoToSimpleResponse(any(EnvironmentDto.class));
        verify(environment, never()).setStatus(any());
        verify(notificationService, never()).send(any(ResourceEvent.class), any(SimpleEnvironmentResponse.class), any());
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());

        verifyDeleteActionSuccessEvent(DELETE_DATALAKE_CLUSTERS_EVENT);
    }

    @Test
    void datalakeClustersDeleteAction() {
        Action<?, ?> action = configureAction(() -> underTest.datalakeClustersDeleteAction());

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        when(environmentService.save(environment)).thenReturn(savedEnvironment);
        when(environmentService.getEnvironmentDto(savedEnvironment)).thenReturn(environmentDto);
        when(environmentResponseConverter.dtoToSimpleResponse(environmentDto)).thenReturn(simpleEnvironmentResponse);

        action.execute(context);

        verify(environment).setStatus(EnvironmentStatus.DATALAKE_CLUSTERS_DELETE_IN_PROGRESS);
        verify(notificationService).send(ResourceEvent.ENVIRONMENT_DATALAKE_CLUSTERS_DELETION_STARTED, simpleEnvironmentResponse, FLOW_TRIGGER_USER_CRN);
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());

        verifyDeleteActionSuccessEvent(DELETE_DATALAKE_CLUSTERS_EVENT);
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
        assertThat(payload).isInstanceOf(EnvironmentDto.class);

        EnvironmentDto environmentDto = (EnvironmentDto) payload;
        assertThat(environmentDto.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(environmentDto.getName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(environmentDto.getId()).isEqualTo(ENVIRONMENT_ID);
    }

    private void verifyEventFactoryAndHeaders() {
        assertThat(eventArgumentCaptor.getValue()).isSameAs(event);

        Map<String, Object> headers = headersArgumentCaptor.getValue();
        assertThat(headers).isNotNull();
        assertThat(headers.get(FlowConstants.FLOW_ID)).isEqualTo(FLOW_ID);
        assertThat(headers.get(FlowConstants.FLOW_TRIGGER_USERCRN)).isEqualTo(FLOW_TRIGGER_USER_CRN);
    }
}
