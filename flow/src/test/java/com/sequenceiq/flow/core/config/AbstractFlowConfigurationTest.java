package com.sequenceiq.flow.core.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

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
import org.springframework.statemachine.config.builders.StateMachineConfigurationBuilder;
import org.springframework.statemachine.config.builders.StateMachineStateBuilder;
import org.springframework.statemachine.config.builders.StateMachineTransitionBuilder;
import org.springframework.statemachine.config.common.annotation.ObjectPostProcessor;
import org.springframework.statemachine.listener.StateMachineListener;

import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowEventListener;
import com.sequenceiq.flow.core.FlowFinalizeAction;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.FlowEdgeConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.listener.FlowEventCommonListener;
import com.sequenceiq.flow.core.listener.FlowTransitionContext;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

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

    @Mock
    private FlowEventCommonListener<State, Event> flowEventCommonListener;

    @Mock
    private StateMachineListener<State, Event> stateMachineListener;

    private Flow flow;

    private List<Transition<State, Event>> transitions;

    private FlowEdgeConfig<State, Event> edgeConfig;

    @Before
    public void setup() throws Exception {
        underTest = new TestFlowConfiguration();
        MockitoAnnotations.initMocks(this);
        BDDMockito.given(applicationContext.getBean(ArgumentMatchers.anyString(), ArgumentMatchers.any(Class.class))).willReturn(action);
        BDDMockito.given(applicationContext.getBean(ArgumentMatchers.eq(FlowEventListener.class), ArgumentMatchers.eq(State.INIT),
                        ArgumentMatchers.eq(State.FINAL), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                        ArgumentMatchers.eq("flowChainId"), ArgumentMatchers.eq("flowId"), ArgumentMatchers.anyLong()))
                .willReturn(flowEventListener);
        BDDMockito.given(applicationContext.getBean(ArgumentMatchers.eq(FlowEventCommonListener.class), any(FlowTransitionContext.class)))
                .willReturn(flowEventCommonListener);
        transitions = new Builder<State, Event>()
                .defaultFailureEvent(Event.FAILURE)
                .from(State.INIT).to(State.DO).event(Event.START).noFailureEvent()
                .from(State.DO).to(State.DO2).event(Event.CONTINUE).defaultFailureEvent()
                .from(State.DO2).to(State.FINISH).event(Event.FINISHED).failureState(State.FAILED2).failureEvent(Event.FAILURE2)
                .from(State.FINISH).to(State.FINAL).event(Event.FINALIZED).defaultFailureEvent()
                .build();
        edgeConfig = new FlowEdgeConfig<>(State.INIT, State.FINAL, State.FAILED, Event.FAIL_HANDLED);
        ((AbstractFlowConfiguration<State, Event>) underTest).init();
        verify(applicationContext, Mockito.times(8)).getBean(ArgumentMatchers.anyString(), ArgumentMatchers.any(Class.class));
        flow = underTest.createFlow("flowId", "flowChainId", 0L, "flowChainType");
        flow.initialize(Map.of());
    }

    @Test
    public void testHappyFlowConfiguration() {
        flow.sendEvent(flowParameters(Event.START.name()));
        flow.sendEvent(flowParameters(Event.CONTINUE.name()));
        flow.sendEvent(flowParameters(Event.FINISHED.name()));
        flow.sendEvent(flowParameters(Event.FINALIZED.name()));
    }

    @Test
    public void testUnhappyFlowConfigurationWithDefaultFailureHandler() {
        flow.sendEvent(flowParameters(Event.START.name()));
        flow.sendEvent(flowParameters(Event.FAILURE.name()));
        flow.sendEvent(flowParameters(Event.FAIL_HANDLED.name()));
    }

    @Test
    public void testUnhappyFlowConfigurationWithCustomFailureHandler() {
        flow.sendEvent(flowParameters(Event.START.name()));
        flow.sendEvent(flowParameters(Event.CONTINUE.name()));
        flow.sendEvent(flowParameters(Event.FAILURE2.name()));
        assertEquals("Must be on the FAILED2 state", State.FAILED2, flow.getCurrentState());
        flow.sendEvent(flowParameters(Event.FAIL_HANDLED.name()));
    }

    @Test
    public void testUnacceptedFlowConfiguration1() {
        flow.sendEvent(flowParameters(Event.START.name()));
        flow.sendEvent(flowParameters(Event.FINISHED.name()));

        verify(stateMachineListener).eventNotAccepted(any());
    }

    @Test
    public void testUnacceptedFlowConfiguration2() {
        flow.sendEvent(flowParameters(Event.START.name()));
        flow.sendEvent(flowParameters(Event.FAILURE2.name()));

        verify(stateMachineListener).eventNotAccepted(any());
    }

    @Test
    public void testUnacceptedFlowConfiguration3() {
        flow.sendEvent(flowParameters(Event.START.name()));
        flow.sendEvent(flowParameters(Event.CONTINUE.name()));
        flow.sendEvent(flowParameters(Event.FAIL_HANDLED.name()));

        verify(stateMachineListener).eventNotAccepted(any());
    }

    @Test
    public void testUnacceptedFlowConfiguration4() {
        flow.sendEvent(flowParameters(Event.START.name()));
        flow.sendEvent(flowParameters(Event.CONTINUE.name()));
        flow.sendEvent(flowParameters(Event.FAILURE.name()));

        verify(stateMachineListener).eventNotAccepted(any());
    }

    private FlowParameters flowParameters(String key) {
        return new FlowParameters(null, null, null, null, key, null, null, null);
    }

    enum State implements FlowState {
        INIT, DO, DO2, FINISH, FAILED, FAILED2, FINAL;

        @Override
        public Class<? extends AbstractAction<?, ?, ?, ?>> action() {
            return FlowFinalizeAction.class;
        }

        @Override
        public Class<? extends RestartAction> restartAction() {
            return DefaultRestartAction.class;
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
            return new MachineConfiguration<>(configurationBuilder, stateBuilder, transitionBuilder, stateMachineListener,
                    new SyncTaskExecutor());
        }

        @Override
        protected List<Transition<State, Event>> getTransitions() {
            return transitions;
        }

        @Override
        public FlowEdgeConfig<State, Event> getEdgeConfig() {
            return edgeConfig;
        }

        @Override
        public Event[] getEvents() {
            return EVENTS;
        }

        @Override
        public Event[] getInitEvents() {
            return new Event[] {Event.START};
        }

        @Override
        public String getDisplayName() {
            return "Test flow configuration";
        }
    }
}
