package com.sequenceiq.cloudbreak.core.flow2.config;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.statemachine.config.builders.StateMachineConfigurationBuilder;
import org.springframework.statemachine.config.builders.StateMachineStateBuilder;
import org.springframework.statemachine.config.builders.StateMachineTransitionBuilder;
import org.springframework.statemachine.config.common.annotation.ObjectPostProcessor;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;

import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowFinalizeAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public class AbstractFlowConfigurationTest {

    @InjectMocks
    private FlowConfiguration underTest;

    @Mock
    private ApplicationContext applicationContext;

    private Flow<State, Event> flow;
    private List<FlowConfiguration.Transition<State, Event>> transitions;
    private FlowConfiguration.FlowEdgeConfig<State, Event> edgeConfig;

    @Before
    public void setup() throws Exception {
        underTest = new FlowConfiguration();
        MockitoAnnotations.initMocks(this);
        given(applicationContext.getBean(any(Class.class))).willReturn(new FlowFinalizeAction());
        transitions = Arrays.asList(new FlowConfiguration.Transition<>(State.INIT, State.DO, Event.START),
                new FlowConfiguration.Transition<>(State.DO, State.DO2, Event.CONTINUE),
                new FlowConfiguration.Transition<>(State.DO2, State.FINISH, Event.FINISHED));
        edgeConfig = new FlowConfiguration.FlowEdgeConfig(State.INIT, State.FINAL, State.FINISH, Event.FINALIZED, State.FAILED, Event.FAIL_HANDLED);
        underTest.init();
        verify(applicationContext, times(8)).getBean(anyString(), any(Class.class));
        flow = underTest.createFlow("flowId");
        flow.initialize();
    }

    @Test
    public void testHappyFlowConfiguration() {
        flow.sendEvent(Event.START.name(), null);
        flow.sendEvent(Event.CONTINUE.name(), null);
        flow.sendEvent(Event.FINISHED.name(), null);
        flow.sendEvent(Event.FINALIZED.name(), null);
    }

    @Test
    public void testUnhappyFlowConfigurationWithDefaultFailureHandler() {
        flow.sendEvent(Event.START.name(), null);
        flow.sendEvent(Event.FAILURE.name(), null);
        flow.sendEvent(Event.FAIL_HANDLED.name(), null);
    }

    @Test
    public void testUnhappyFlowConfigurationWithCustomFailureHandler() {
        flow.sendEvent(Event.START.name(), null);
        flow.sendEvent(Event.CONTINUE.name(), null);
        flow.sendEvent(Event.FAILURE2.name(), null);
        flow.sendEvent(Event.FAIL_HANDLED.name(), null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnacceptedFlowConfiguration1() {
        flow.sendEvent(Event.START.name(), null);
        flow.sendEvent(Event.FINISHED.name(), null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnacceptedFlowConfiguration2() {
        flow.sendEvent(Event.START.name(), null);
        flow.sendEvent(Event.FAILURE2.name(), null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnacceptedFlowConfiguration3() {
        flow.sendEvent(Event.START.name(), null);
        flow.sendEvent(Event.CONTINUE.name(), null);
        flow.sendEvent(Event.FAIL_HANDLED.name(), null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnacceptedFlowConfiguration4() {
        flow.sendEvent(Event.START.name(), null);
        flow.sendEvent(Event.CONTINUE.name(), null);
        flow.sendEvent(Event.FAILURE.name(), null);
    }

    enum State implements FlowState<State, Event> {
        INIT, DO, DO2, FINISH, FAILED, FAILED2, FINAL;

        @Override
        public Class<?> action() {
            return FlowFinalizeAction.class;
        }

        @Override
        public State failureState() {
            return this == DO2 ? FAILED2 : FAILED;
        }

        @Override
        public Event failureEvent() {
            return this == DO2 ? Event.FAILURE2 : Event.FAILURE;
        }
    }

    enum Event implements FlowEvent {
        START, CONTINUE, FINISHED, FAILURE, FAILURE2, FINALIZED, FAIL_HANDLED;

        public String stringRepresentation() {
            return name();
        }
    }

    class FlowConfiguration extends AbstractFlowConfiguration<State, Event> {

        private FlowConfiguration() {
            super(State.class, Event.class);
        }

        @Override
        protected FlowConfiguration.MachineConfiguration<State, Event> getStateMachineConfiguration() {
            StateMachineConfigurationBuilder<State, Event> configurationBuilder =
                    new StateMachineConfigurationBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
            StateMachineStateBuilder<State, Event> stateBuilder =
                    new StateMachineStateBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
            StateMachineTransitionBuilder<State, Event> transitionBuilder =
                    new StateMachineTransitionBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
            StateMachineListener<State, Event> listener =
                    new StateMachineListenerAdapter<State, Event>() {
                        @Override
                        public void eventNotAccepted(Message<Event> event) {
                            throw new UnsupportedOperationException();
                        }
                    };
            return new FlowConfiguration.MachineConfiguration<>(configurationBuilder, stateBuilder, transitionBuilder, listener, new SyncTaskExecutor());
        }

        @Override
        protected List<Transition<State, Event>> getTransitions() {
            return transitions;
        }

        @Override
        protected FlowEdgeConfig<State, Event> getEdgeConfig() {
            return edgeConfig;
        }

        @Override
        public Event[] getEvents() {
            return new Event[0];
        }
    }
}
