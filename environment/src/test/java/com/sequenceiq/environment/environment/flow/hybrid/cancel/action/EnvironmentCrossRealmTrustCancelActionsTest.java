package com.sequenceiq.environment.environment.flow.hybrid.cancel.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.state.State;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelEvent;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.environment.metrics.MetricType;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.MessageFactory;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class EnvironmentCrossRealmTrustCancelActionsTest {

    private static final String FLOW_ID = "flowId";

    private static final String FLOW_TRIGGER_USER_CRN = "flowTriggerUserCrn";

    @Mock
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EnvironmentMetricService metricService;

    @Mock
    private MetricService commonMetricsService;

    @Mock
    private EnvironmentDto environmentDto;

    @Mock
    private StateContext stateContext;

    @Mock
    private StateMachine stateMachine;

    @Mock
    private FlowEvent failureEvent;

    @Mock
    private State state;

    @Mock
    private Event<Object> event;

    @Mock
    private ExtendedState extendedState;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private EnvironmentCrossRealmTrustCancelEvent actionPayload;

    @InjectMocks
    private EnvironmentCrossRealmTrustCancelActions actions;

    private CommonContext commonContext;

    @BeforeEach
    void setUp() {
        FlowParameters flowParameters = new FlowParameters(FLOW_ID, FLOW_TRIGGER_USER_CRN);
        commonContext = new CommonContext(flowParameters);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_PARAMETERS.name())).thenReturn(flowParameters);
        when(runningFlows.getFlowChainId(anyString())).thenReturn(null);

        actions = new EnvironmentCrossRealmTrustCancelActions(environmentStatusUpdateService, metricService);

        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(actions);
        when(stateContext.getExtendedState()).thenReturn(extendedState);
        when(extendedState.getVariables()).thenReturn(new HashMap<>());
        when(stateContext.getStateMachine()).thenReturn(stateMachine);
        when(stateMachine.getState()).thenReturn(state);
        when(reactorEventFactory.createEvent(any(), any())).thenReturn(event);
        doNothing().when(eventBus).notify(any(), any());
    }

    @Test
    void testCrossRealmTrustCancelValidationAction() {
        Action<?, ?> action = configureAction(() -> actions.crossRealmTrustCancelValidationAction());
        action.execute(stateContext);

        verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void testCrossRealmTrustCancelInFreeIpaAction() throws Exception {
        Action<?, ?> action = configureAction(() -> actions.crossRealmTrustCancelInFreeIpaAction());

        action.execute(stateContext);

        verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void testFinishedAction() {
        when(environmentStatusUpdateService.updateEnvironmentStatusAndNotify(
                any(), any(), any(), any(), any()))
                .thenReturn(environmentDto);

        Action<?, ?> action = configureAction(() -> actions.finishedAction());
        action.execute(stateContext);

        verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verify(metricService).incrementMetricCounter(MetricType.ENV_TRUST_CANCEL_FINISHED, environmentDto);
    }

    @Test
    void testFailedAction() throws Exception {
        EnvironmentCrossRealmTrustCancelFailedEvent payload = mock(EnvironmentCrossRealmTrustCancelFailedEvent.class);

        EnvironmentStatus failedStatus = EnvironmentStatus.TRUST_CANCEL_VALIDATION_FAILED;
        Exception ex = new RuntimeException("failure");

        when(environmentStatusUpdateService.updateFailedEnvironmentStatusAndNotify(
                any(), any(), any(), any(), any())).thenReturn(environmentDto);
        when(payload.getEnvironmentStatus()).thenReturn(failedStatus);
        when(payload.getException()).thenReturn(ex);

        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(payload);

        Action<?, ?> action = configureAction(() -> actions.failedAction());

        action.execute(stateContext);

        verify(environmentStatusUpdateService).updateFailedEnvironmentStatusAndNotify(
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verify(metricService).incrementMetricCounter(
                MetricType.ENV_TRUST_CANCEL_FAILED,
                environmentDto,
                ex
        );
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
        ReflectionTestUtils.setField(action, null, commonMetricsService, MetricService.class);
        ReflectionTestUtils.setField(action, null, new ArrayList<>(), List.class);
    }
}
