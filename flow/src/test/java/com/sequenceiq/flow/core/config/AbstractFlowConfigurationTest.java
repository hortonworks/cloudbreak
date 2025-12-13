package com.sequenceiq.flow.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import com.sequenceiq.flow.core.FlowEventContext;
import com.sequenceiq.flow.core.FlowEventListener;
import com.sequenceiq.flow.core.FlowFinalizeAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.FlowEdgeConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.listener.FlowEventCommonListener;
import com.sequenceiq.flow.core.listener.FlowTransitionContext;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

@ExtendWith(MockitoExtension.class)
class AbstractFlowConfigurationTest {

    private static final Event[] EVENTS = new Event[0];

    @InjectMocks
    private FlowConfiguration<?> underTest = new TestFlowConfiguration();

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

    @BeforeEach
    void setup() throws Exception {
        when(applicationContext.getBean(anyString(), any(Class.class))).thenReturn(action);
        when(applicationContext.getBean(eq(FlowEventListener.class), eq(State.INIT), eq(State.FINAL), anyString(),
                anyString(), eq("flowChainId"), eq("flowId"), anyLong()))
                .thenReturn(flowEventListener);
        when(applicationContext.getBean(eq(FlowEventCommonListener.class), any(FlowTransitionContext.class))).thenReturn(flowEventCommonListener);
        transitions = new Builder<State, Event>()
                .defaultFailureEvent(Event.FAILURE)
                .from(State.INIT).to(State.DO).event(Event.START).noFailureEvent()
                .from(State.DO).to(State.DO2).event(Event.CONTINUE).defaultFailureEvent()
                .from(State.DO2).to(State.FINISH).event(Event.FINISHED).failureState(State.FAILED2).failureEvent(Event.FAILURE2)
                .from(State.FINISH).to(State.FINAL).event(Event.FINALIZED).defaultFailureEvent()
                .build();
        edgeConfig = new FlowEdgeConfig<>(State.INIT, State.FINAL, State.FAILED, Event.FAIL_HANDLED);
        ((AbstractFlowConfiguration<State, Event>) underTest).init();
        verify(applicationContext, times(8)).getBean(anyString(), any(Class.class));
        flow = underTest.createFlow("flowId", "flowChainId", 0L, "flowChainType");
        flow.initialize(Map.of());
    }

    @Test
    void testHappyFlowConfiguration() {
        flow.sendEvent(flowEventContext(Event.START.name()));
        flow.sendEvent(flowEventContext(Event.CONTINUE.name()));
        flow.sendEvent(flowEventContext(Event.FINISHED.name()));
        flow.sendEvent(flowEventContext(Event.FINALIZED.name()));
    }

    @Test
    void testUnhappyFlowConfigurationWithDefaultFailureHandler() {
        flow.sendEvent(flowEventContext(Event.START.name()));
        flow.sendEvent(flowEventContext(Event.FAILURE.name()));
        flow.sendEvent(flowEventContext(Event.FAIL_HANDLED.name()));
    }

    @Test
    void testUnhappyFlowConfigurationWithCustomFailureHandler() {
        flow.sendEvent(flowEventContext(Event.START.name()));
        flow.sendEvent(flowEventContext(Event.CONTINUE.name()));
        flow.sendEvent(flowEventContext(Event.FAILURE2.name()));
        assertEquals(State.FAILED2, flow.getCurrentState(), "Must be on the FAILED2 state");
        flow.sendEvent(flowEventContext(Event.FAIL_HANDLED.name()));
    }

    @Test
    void testUnacceptedFlowConfiguration1() {
        flow.sendEvent(flowEventContext(Event.START.name()));
        flow.sendEvent(flowEventContext(Event.FINISHED.name()));

        verify(stateMachineListener).eventNotAccepted(any());
    }

    @Test
    void testUnacceptedFlowConfiguration2() {
        flow.sendEvent(flowEventContext(Event.START.name()));
        flow.sendEvent(flowEventContext(Event.FAILURE2.name()));

        verify(stateMachineListener).eventNotAccepted(any());
    }

    @Test
    void testUnacceptedFlowConfiguration3() {
        flow.sendEvent(flowEventContext(Event.START.name()));
        flow.sendEvent(flowEventContext(Event.CONTINUE.name()));
        flow.sendEvent(flowEventContext(Event.FAIL_HANDLED.name()));

        verify(stateMachineListener).eventNotAccepted(any());
    }

    @Test
    void testUnacceptedFlowConfiguration4() {
        flow.sendEvent(flowEventContext(Event.START.name()));
        flow.sendEvent(flowEventContext(Event.CONTINUE.name()));
        flow.sendEvent(flowEventContext(Event.FAILURE.name()));

        verify(stateMachineListener).eventNotAccepted(any());
    }

    private FlowEventContext flowEventContext(String key) {
        return new FlowEventContext(null, null, null, null, key, null, null);
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
            return new Event[]{Event.START};
        }

        @Override
        public String getDisplayName() {
            return "Test flow configuration";
        }
    }
}
