package com.sequenceiq.cloudbreak.core.flow2;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.ObjectStateMachineFactory;
import org.springframework.statemachine.config.builders.StateMachineConfigurationBuilder;
import org.springframework.statemachine.config.builders.StateMachineStateBuilder;
import org.springframework.statemachine.config.builders.StateMachineTransitionBuilder;
import org.springframework.statemachine.config.common.annotation.ObjectPostProcessor;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;

import reactor.bus.EventBus;

public class AbstractActionTest {

    public static final String FLOW_ID = "flowId";

    @InjectMocks
    private TestAction underTest;

    @Mock
    private EventBus eventBus;

    private StateMachine<State, Event> stateMachine;

    @Before
    public void setup() throws Exception {
        underTest = spy(new TestAction());
        MockitoAnnotations.initMocks(this);
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
    public void testExecute() throws Exception {
        stateMachine.sendEvent(Event.DOIT);
        verify(underTest, times(1)).createFlowContext(any(StateContext.class), any(Payload.class));
        verify(underTest, times(1)).doExecute(any(CommonContext.class), any(Payload.class), any(Map.class));
        verify(underTest, times(0)).sendEvent(any(CommonContext.class));
        verify(underTest, times(0)).sendEvent(anyString(), anyString(), any());
        verify(underTest, times(0)).sendEvent(anyString(), any(Selectable.class));
        verify(underTest, times(0)).getFailurePayload(any(CommonContext.class), any(RuntimeException.class));
    }

    @Test
    public void testFailedExecute() throws Exception {
        RuntimeException exception = new UnsupportedOperationException();
        Mockito.doThrow(exception).when(underTest).doExecute(any(CommonContext.class), any(Payload.class), any(Map.class));
        stateMachine.sendEvent(Event.DOIT);
        verify(underTest, times(1)).createFlowContext(any(StateContext.class), any(Payload.class));
        verify(underTest, times(1)).doExecute(any(CommonContext.class), any(Payload.class), any(Map.class));
        verify(underTest, times(1)).getFailurePayload(any(CommonContext.class), eq(exception));
        verify(underTest, times(1)).sendEvent(eq(FLOW_ID), eq(Event.FAILURE.name()), eq(Collections.emptyMap()));
    }

    enum State implements FlowState<State, Event> {
        INIT, DOING, FAILED;

        @Override
        public Class<?> action() {
            return TestAction.class;
        }

        @Override
        public State failureState() {
            return FAILED;
        }

        @Override
        public Event failureEvent() {
            return Event.FAILURE;
        }

        @Override
        public void setFailureEvent(Event failureEvent) {
        }
    }

    enum Event implements FlowEvent {
        DOIT, FAILURE;

        public String stringRepresentation() {
            return name();
        }
    }

    class TestAction extends AbstractAction<State, Event, CommonContext, Payload> {

        protected TestAction() {
            super(Payload.class);
        }

        @Override
        public CommonContext createFlowContext(StateContext<State, Event> stateContext, Payload payload) {
            return new CommonContext(FLOW_ID);
        }

        @Override
        public void doExecute(CommonContext context, Payload payload, Map<Object, Object> variables) throws Exception {
        }

        @Override
        protected Selectable createRequest(CommonContext context) {
            return null;
        }

        @Override
        public Object getFailurePayload(CommonContext flowContext, Exception ex) {
            return Collections.emptyMap();
        }
    }

}
