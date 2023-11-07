package com.sequenceiq.environment.environment.flow.deletion;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_ENVIRONMENT_RESOURCE_ENCRYPTION_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_IDBROKER_MAPPINGS_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_PUBLICKEY_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentResponseConverter;
import com.sequenceiq.environment.events.EventSenderService;
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
class EnvDeleteActionsTest {

    private static final String FLOW_ID = "flowId";

    private static final String FLOW_TRIGGER_USER_CRN = "flowTriggerUserCrn";

    private static final String ACTION_PAYLOAD_SELECTOR = "selector";

    private static final Long ENVIRONMENT_ID = 1234L;

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final boolean FORCE_DELETE_ENABLED = true;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EventSenderService eventService;

    @Mock
    private EnvironmentResponseConverter environmentResponseConverter;

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
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @Mock
    private EnvironmentMetricService metricService;

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

    @Mock
    private FlowEvent flowEvent;

    @BeforeEach
    void setUp() {
        FlowParameters flowParameters = new FlowParameters(FLOW_ID, FLOW_TRIGGER_USER_CRN);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_PARAMETERS.name())).thenReturn(flowParameters);
        actionPayload = new EnvDeleteEvent(ACTION_PAYLOAD_SELECTOR, ENVIRONMENT_ID, ENVIRONMENT_NAME, ENVIRONMENT_CRN, FORCE_DELETE_ENABLED);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(actionPayload);
        when(stateContext.getExtendedState()).thenReturn(extendedState);
        when(extendedState.getVariables()).thenReturn(new HashMap<>());
        when(stateContext.getStateMachine()).thenReturn(stateMachine);
        when(stateMachine.getState()).thenReturn(state);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);
    }

    @Test
    void idbmmsDeleteActionTestHappyPath() {
        testDeleteActionHappyPath(underTest::idbmmsDeleteAction, DELETE_IDBROKER_MAPPINGS_EVENT.selector(),
                EnvironmentStatus.IDBROKER_MAPPINGS_DELETE_IN_PROGRESS, ResourceEvent.ENVIRONMENT_IDBROKER_MAPPINGS_DELETION_STARTED,
                EnvDeleteState.IDBROKER_MAPPINGS_DELETE_STARTED_STATE);
    }

    @Test
    void publicKeyDeleteActionTestHappyPath() {
        testDeleteActionHappyPath(underTest::publickeyDeleteAction, DELETE_PUBLICKEY_EVENT.selector(),
                EnvironmentStatus.PUBLICKEY_DELETE_IN_PROGRESS, ResourceEvent.ENVIRONMENT_PUBLICKEY_DELETION_STARTED,
                EnvDeleteState.PUBLICKEY_DELETE_STARTED_STATE);
    }

    @Test
    void resourceEncryptionDeleteActionTestHappyPath() {
        testDeleteActionHappyPath(underTest::resourceEncryptionDeleteAction, DELETE_ENVIRONMENT_RESOURCE_ENCRYPTION_EVENT.selector(),
                EnvironmentStatus.ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_IN_PROGRESS, ResourceEvent.ENVIRONMENT_RESOURCE_ENCRYPTION_DELETION_STARTED,
                EnvDeleteState.ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_STARTED_STATE);
    }

    private void testDeleteActionHappyPath(Supplier<Action<?, ?>> deleteAction,
            String selector,
            EnvironmentStatus environmentStatus,
            ResourceEvent eventStarted, EnvDeleteState envDeleteState) {

        Action<?, ?> action = configureAction(deleteAction);

        EnvironmentDto environmentDto = mock(EnvironmentDto.class);
        when(environmentStatusUpdateService.updateEnvironmentStatusAndNotify(any(), any(), any(), any(), any())).thenReturn(environmentDto);

        action.execute(stateContext);

        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());
        verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(any(), any(), eq(environmentStatus), eq(eventStarted), eq(envDeleteState));

        verifyDeleteActionSuccessEvent(selector, environmentDto);
    }

    private void verifyDeleteActionSuccessEvent(String selector, EnvironmentDto environmentDto) {
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(selector);

        verifyEventFactoryAndHeaders();

        Object payload = payloadArgumentCaptor.getValue();
        assertThat(payload).isInstanceOf(EnvironmentDeletionDto.class);

        EnvironmentDeletionDto environmentDeletionDto = (EnvironmentDeletionDto) payload;
        assertThat(environmentDeletionDto.getEnvironmentDto()).isSameAs(environmentDto);
        assertThat(environmentDeletionDto.isForceDelete()).isEqualTo(FORCE_DELETE_ENABLED);
        assertThat(environmentDeletionDto.getResourceId()).isEqualTo(ENVIRONMENT_ID);
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

    private void verifyEventFactoryAndHeaders() {
        assertThat(eventArgumentCaptor.getValue()).isSameAs(event);

        Map<String, Object> headers = headersArgumentCaptor.getValue();
        assertThat(headers).isNotNull();
        assertThat(headers.get(FlowConstants.FLOW_ID)).isEqualTo(FLOW_ID);
        assertThat(headers.get(FlowConstants.FLOW_TRIGGER_USERCRN)).isEqualTo(FLOW_TRIGGER_USER_CRN);
    }

}
