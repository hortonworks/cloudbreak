package com.sequenceiq.cloudbreak.core.flow2;

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

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.bus.EventBus;

@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
public class AbstractActionTest {

    public static final String FLOW_ID = "flowId";

    @InjectMocks
    private TestAction underTest;

    @Mock
    private EventBus eventBus;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private Flow flow;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    private StateMachine<State, Event> stateMachine;

    @Before
    public void setup() throws Exception {
        underTest = spy(new TestAction());
        underTest.setFailureEvent(Event.FAILURE);
        MockitoAnnotations.initMocks(this);
        BDDMockito.given(flow.getFlowId()).willReturn(FLOW_ID);
        BDDMockito.given(runningFlows.get(anyString())).willReturn(flow);
        StateMachineConfigurationBuilder<State, Event> configurationBuilder =
                new StateMachineConfigurationBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        configurationBuilder.setTaskExecutor(new SyncTaskExecutor());
        StateMachineStateBuilder<State, Event> stateBuilder =
                new StateMachineStateBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        stateBuilder.withStates().initial(State.INIT).state(State.DOING, underTest, null);
        StateMachineTransitionBuilder<State, Event> transitionBuilder =
                new StateMachineTransitionBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        transitionBuilder.withExternal().source(State.INIT).target(State.DOING).event(Event.DOIT);
        stateMachine = new ObjectStateMachineFactory<>(configurationBuilder.build(), transitionBuilder.build(), stateBuilder.build()).getStateMachine();
        stateMachine.start();
    }

    @Test
    public void testExecute() {
        stateMachine.sendEvent(new GenericMessage<>(Event.DOIT, Collections.singletonMap(Flow2Handler.FLOW_ID, FLOW_ID)));
        verify(underTest, times(1)).createFlowContext(eq(FLOW_ID), any(StateContext.class), nullable(Payload.class));
        verify(underTest, times(1)).doExecute(any(CommonContext.class), nullable(Payload.class), any(Map.class));
        verify(underTest, times(0)).sendEvent(any(CommonContext.class));
        verify(underTest, times(0)).sendEvent(anyString(), anyString(), any());
        verify(underTest, times(0)).sendEvent(anyString(), any(Selectable.class));
        verify(underTest, times(0)).getFailurePayload(any(Payload.class), any(Optional.class), any(RuntimeException.class));
    }

    @Test
    public void testFailedExecute() {
        RuntimeException exception = new UnsupportedOperationException("");
        Mockito.doThrow(exception).when(underTest).doExecute(any(CommonContext.class), nullable(Payload.class), any());
        stateMachine.sendEvent(new GenericMessage<>(Event.DOIT, Collections.singletonMap(Flow2Handler.FLOW_ID, FLOW_ID)));
        verify(underTest, times(1)).createFlowContext(eq(FLOW_ID), any(StateContext.class), nullable(Payload.class));
        verify(underTest, times(1)).doExecute(any(CommonContext.class), nullable(Payload.class), any(Map.class));
        verify(underTest, times(1)).getFailurePayload(nullable(Payload.class), any(Optional.class), eq(exception));
        verify(underTest, times(1)).sendEvent(eq(FLOW_ID), eq(Event.FAILURE.name()), eq(Collections.emptyMap()));
    }

    enum State implements FlowState {
        INIT, DOING;

        @Override
        public Class<? extends AbstractAction<?, ?, ?, ?>> action() {
            return TestAction.class;
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
        public CommonContext createFlowContext(String flowId, StateContext<State, Event> stateContext, Payload payload) {
            return new CommonContext(FLOW_ID);
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
