package com.sequenceiq.flow.core;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.ObjectStateMachineFactory;
import org.springframework.statemachine.config.builders.StateMachineConfigurationBuilder;
import org.springframework.statemachine.config.builders.StateMachineStateBuilder;
import org.springframework.statemachine.config.builders.StateMachineTransitionBuilder;
import org.springframework.statemachine.config.common.annotation.ObjectPostProcessor;
import org.springframework.statemachine.config.model.ConfigurationData;
import org.springframework.statemachine.config.model.DefaultStateMachineModel;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.FlowEdgeConfig;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
public class AbstractActionTest {

    public static final String FLOW_ID = "flowId";

    public static final String FLOW_TRIGGER_USERCRN = "crn:cdp:iam:us-west-1:cloudera:user:ausername";

    public static final FlowParameters FLOW_PARAMETERS = new FlowParameters(FLOW_ID, FLOW_TRIGGER_USERCRN);

    @InjectMocks
    private TestAction underTest;

    @Mock
    private EventBus eventBus;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private MetricService metricService;

    @Mock
    private MetricService commonMetricsService;

    @Mock
    private Flow flow;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowLogDBService flowLogDBService;

    private StateMachine<State, Event> stateMachine;

    @Before
    public void setup() throws Exception {
        underTest = spy(new TestAction());
        MockitoAnnotations.initMocks(this);
        BDDMockito.given(flow.getFlowId()).willReturn(FLOW_ID);
        BDDMockito.given(runningFlows.get(anyString())).willReturn(flow);
        StateMachineConfigurationBuilder<State, Event> configurationBuilder =
                new StateMachineConfigurationBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        configurationBuilder.setTaskExecutor(new SyncTaskExecutor());
        StateMachineStateBuilder<State, Event> stateBuilder =
                new StateMachineStateBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        stateBuilder.withStates().initial(State.INIT).state(State.DOING, underTest, null).state(State.FAILED_STATE, underTest, null);
        StateMachineTransitionBuilder<State, Event> transitionBuilder =
                new StateMachineTransitionBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        transitionBuilder.withExternal().source(State.INIT).target(State.DOING).event(Event.DOIT);
        transitionBuilder.withExternal().source(State.DOING).target(State.FAILED_STATE).event(Event.FAILURE);
        ConfigurationData<State, Event> configurationData = configurationBuilder.build();
        transitionBuilder.setSharedObject(ConfigurationData.class, configurationData);
        stateMachine = new ObjectStateMachineFactory<>(
                new DefaultStateMachineModel<>(configurationData, stateBuilder.build(), transitionBuilder.build())).getStateMachine();
        stateMachine.start();
    }

    @Test
    public void testExecute() {
        underTest.setFailureEvent(Event.FAILURE);
        stateMachine.sendEvent(new GenericMessage<>(Event.DOIT, Collections.singletonMap(FlowConstants.FLOW_PARAMETERS, FLOW_PARAMETERS)));
        verify(underTest, times(1)).createFlowContext(eq(FLOW_PARAMETERS), any(StateContext.class), nullable(Payload.class));
        verify(underTest, times(1)).doExecute(any(CommonContext.class), nullable(Payload.class), any(Map.class));
        verify(underTest, times(0)).sendEvent(any(CommonContext.class));
        verify(underTest, times(0)).sendEvent(eq(FLOW_PARAMETERS), anyString(), any(), any(Map.class));
        verify(underTest, times(0)).sendEvent(eq(FLOW_PARAMETERS), anyString(), any(Selectable.class), any(Map.class));
        verify(underTest, times(0)).getFailurePayload(any(Payload.class), any(Optional.class), any(RuntimeException.class));
    }

    @Test
    public void testFailedExecute() {
        underTest.setFailureEvent(Event.FAILURE);
        RuntimeException exception = new UnsupportedOperationException("");
        Mockito.doThrow(exception).when(underTest).doExecute(any(CommonContext.class), nullable(Payload.class), any());
        Mockito.doThrow(exception).when(underTest).doExecute(any(CommonContext.class), nullable(Payload.class), any());
        stateMachine.sendEvent(new GenericMessage<>(Event.DOIT, Collections.singletonMap(FlowConstants.FLOW_PARAMETERS, FLOW_PARAMETERS)));
        verify(underTest, times(1)).createFlowContext(eq(FLOW_PARAMETERS), any(StateContext.class), nullable(Payload.class));
        verify(underTest, times(1)).doExecute(any(CommonContext.class), nullable(Payload.class), any(Map.class));
        verify(underTest, times(1)).getFailurePayload(nullable(Payload.class), any(Optional.class), eq(exception));
        verify(underTest, times(1)).sendEvent(eq(FLOW_PARAMETERS), eq(Event.FAILURE.name()), eq(Collections.emptyMap()), any(Map.class));
    }

    @Test
    public void testFailedExecuteWithoutFailureEvent() {
        underTest.setFailureEvent(null);
        underTest.setFlowEdgeConfig(new FlowEdgeConfig<>(State.INIT, State.FINAL, State.FAILED_STATE, null));
        RuntimeException exception = new IllegalStateException("something went wrong");
        Mockito.doThrow(exception).when(underTest).doExecute(any(CommonContext.class), nullable(Payload.class), any());
        stateMachine.sendEvent(new GenericMessage<>(Event.DOIT, Collections.singletonMap(FlowConstants.FLOW_PARAMETERS, FLOW_PARAMETERS)));
        verify(flowLogDBService, times(1)).closeFlowOnError(FLOW_ID, "Operation failed in DOING state without error handler. Message: something went wrong");
    }

    @Test
    public void testFailHandlerExecutionFailure() {
        stateMachine.sendEvent(new GenericMessage<>(Event.DOIT, Collections.singletonMap(FlowConstants.FLOW_PARAMETERS, FLOW_PARAMETERS)));
        underTest.setFailureEvent(null);
        underTest.setFlowEdgeConfig(new FlowEdgeConfig<>(State.INIT, State.FINAL, State.FAILED_STATE, null));
        RuntimeException exception = new IllegalStateException("something went wrong");
        Mockito.doThrow(exception).when(underTest).doExecute(any(CommonContext.class), nullable(Payload.class), any());
        stateMachine.sendEvent(new GenericMessage<>(Event.FAILURE, Collections.singletonMap(FlowConstants.FLOW_PARAMETERS, FLOW_PARAMETERS)));
        verify(flowLogDBService, times(1)).closeFlowOnError(FLOW_ID, "Error handler failed in FAILED_STATE state. Message: something went wrong");
    }

    @Test
    public void testFailedExecuteAndGetFailurePayloadFails() {
        underTest.setFailureEvent(Event.FAILURE);
        RuntimeException exception = new IllegalStateException("something went wrong");
        Mockito.doThrow(exception).when(underTest).doExecute(any(CommonContext.class), nullable(Payload.class), any());
        Mockito.doThrow(new NullPointerException("null")).when(underTest).getFailurePayload(any(), any(), any());
        stateMachine.sendEvent(new GenericMessage<>(Event.DOIT, Collections.singletonMap(FlowConstants.FLOW_PARAMETERS, FLOW_PARAMETERS)));
        verify(flowLogDBService, times(1)).closeFlowOnError(FLOW_ID, "Unhandled exception happened in flow execution, type: " +
                "java.lang.IllegalStateException, message: something went wrong");
    }

    enum State implements FlowState {
        INIT, DOING, FAILED_STATE, FINAL;

        @Override
        public Class<? extends AbstractAction<?, ?, ?, ?>> action() {
            return TestAction.class;
        }

        @Override
        public Class<? extends RestartAction> restartAction() {
            return DefaultRestartAction.class;
        }
    }

    enum Event implements FlowEvent {
        DOIT, FAILURE;

        @Override
        public String event() {
            return name();
        }
    }

    static class TestAction extends AbstractAction<State, Event, CommonContext, Payload> {

        protected TestAction() {
            super(Payload.class);
        }

        @Override
        public CommonContext createFlowContext(FlowParameters flowParameters, StateContext<State, Event> stateContext, Payload payload) {
            return new CommonContext(flowParameters);
        }

        @Override
        public void doExecute(CommonContext context, Payload payload, Map<Object, Object> variables) {
        }

        @Override
        protected Selectable createRequest(CommonContext context) {
            return null;
        }

        @Override
        public Object getFailurePayload(Payload payload, Optional<CommonContext> flowContext, Exception ex) {
            return Collections.emptyMap();
        }
    }
}
