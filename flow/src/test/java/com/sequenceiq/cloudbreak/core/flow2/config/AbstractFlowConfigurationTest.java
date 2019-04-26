package com.sequenceiq.cloudbreak.core.flow2.config;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowEventListener;
import com.sequenceiq.cloudbreak.core.flow2.FlowFinalizeAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration.FlowEdgeConfig;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration.Transition;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfigurationTest.TestFlowConfiguration.NotAcceptedException;

public class AbstractFlowConfigurationTest {

    private static final Event[] EVENTS = new Event[0];

    @InjectMocks
    private FlowConfiguration<?> underTest;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private AbstractAction<State, Event, ?, ?> action;

    @Mock
    private FlowEventListener<State, Event> flowEventListener;

    private Flow flow;

    private List<Transition<State, Event>> transitions;

    private FlowEdgeConfig<State, Event> edgeConfig;

    @Before
    public void setup() throws Exception {
        underTest = new TestFlowConfiguration();
        MockitoAnnotations.initMocks(this);
        BDDMockito.given(applicationContext.getBean(ArgumentMatchers.anyString(), ArgumentMatchers.any(Class.class))).willReturn(action);
        BDDMockito.given(applicationContext.getBean(ArgumentMatchers.eq(FlowEventListener.class), ArgumentMatchers.eq(State.INIT),
                ArgumentMatchers.eq(State.FINAL), ArgumentMatchers.anyString(), ArgumentMatchers.eq("flowId"),
                ArgumentMatchers.anyLong())).willReturn(flowEventListener);
        transitions = new Builder<State, Event>()
                .defaultFailureEvent(Event.FAILURE)
                .from(State.INIT).to(State.DO).event(Event.START).noFailureEvent()
                .from(State.DO).to(State.DO2).event(Event.CONTINUE).defaultFailureEvent()
                .from(State.DO2).to(State.FINISH).event(Event.FINISHED).failureState(State.FAILED2).failureEvent(Event.FAILURE2)
                .from(State.FINISH).to(State.FINAL).event(Event.FINALIZED).defaultFailureEvent()
                .build();
        edgeConfig = new FlowEdgeConfig<>(State.INIT, State.FINAL, State.FAILED, Event.FAIL_HANDLED);
        ((AbstractFlowConfiguration<State, Event>) underTest).init();
        Mockito.verify(applicationContext, Mockito.times(8)).getBean(ArgumentMatchers.anyString(), ArgumentMatchers.any(Class.class));
        flow = underTest.createFlow("flowId", 0L);
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
        assertEquals("Must be on the FAILED2 state", State.FAILED2, flow.getCurrentState());
        flow.sendEvent(Event.FAIL_HANDLED.name(), null);
    }

    @Test(expected = NotAcceptedException.class)
    public void testUnacceptedFlowConfiguration1() {
        flow.sendEvent(Event.START.name(), null);
        flow.sendEvent(Event.FINISHED.name(), null);
    }

    @Test(expected = NotAcceptedException.class)
    public void testUnacceptedFlowConfiguration2() {
        flow.sendEvent(Event.START.name(), null);
        flow.sendEvent(Event.FAILURE2.name(), null);
    }

    @Test(expected = NotAcceptedException.class)
    public void testUnacceptedFlowConfiguration3() {
        flow.sendEvent(Event.START.name(), null);
        flow.sendEvent(Event.CONTINUE.name(), null);
        flow.sendEvent(Event.FAIL_HANDLED.name(), null);
    }

    @Test(expected = NotAcceptedException.class)
    public void testUnacceptedFlowConfiguration4() {
        flow.sendEvent(Event.START.name(), null);
        flow.sendEvent(Event.CONTINUE.name(), null);
        flow.sendEvent(Event.FAILURE.name(), null);
    }

    enum State implements FlowState {
        INIT, DO, DO2, FINISH, FAILED, FAILED2, FINAL;

        @Override
        public Class<? extends AbstractAction<?, ?, ?, ?>> action() {
            return FlowFinalizeAction.class;
        }
    }

    enum Event implements FlowEvent {
        START, CONTINUE, FINISHED, FAILURE, FAILURE2, FINALIZED, FAIL_HANDLED;

        @Override
        public String event() {
            return name();
        }
    }

    class TestFlowConfiguration extends AbstractFlowConfiguration<State, Event> {

        private TestFlowConfiguration() {
            super(State.class, Event.class);
        }

        @Override
        protected MachineConfiguration<State, Event> getStateMachineConfiguration() {
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
                            throw new NotAcceptedException();
                        }
                    };
            return new MachineConfiguration<>(configurationBuilder, stateBuilder, transitionBuilder, listener,
                    new SyncTaskExecutor());
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
            return EVENTS;
        }

        @Override
        public Event[] getInitEvents() {
            return new Event[]{ Event.START };
        }

        class NotAcceptedException extends RuntimeException {

        }
    }
}
