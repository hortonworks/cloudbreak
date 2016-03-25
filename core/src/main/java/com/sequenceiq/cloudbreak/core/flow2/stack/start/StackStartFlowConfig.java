package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.START_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.START_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.START_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.START_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.START_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.START_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.START_STATE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.statemachine.config.builders.StateMachineConfigurationBuilder;
import org.springframework.statemachine.config.builders.StateMachineStateBuilder;
import org.springframework.statemachine.config.builders.StateMachineTransitionBuilder;
import org.springframework.statemachine.config.common.annotation.ObjectPostProcessor;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.MessageFactory;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class StackStartFlowConfig extends AbstractFlowConfiguration<StackStartState, StackStartEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartFlowConfig.class);
    private static final List<Transition<StackStartState, StackStartEvent>> TRANSITIONS = Arrays.asList(
            new Transition<>(INIT_STATE, START_STATE, START_EVENT),
            new Transition<>(START_STATE, START_FINISHED_STATE, START_FINISHED_EVENT)
    );
    private static final FlowEdgeConfig<StackStartState, StackStartEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, START_FINISHED_STATE, START_FINALIZED_EVENT, START_FAILED_STATE, START_FAIL_HANDLED_EVENT);

    @Override
    public Flow<StackStartState, StackStartEvent> createFlow(String flowId) {
        Flow<StackStartState, StackStartEvent> flow = new Flow<>(getStateMachineFactory().getStateMachine(),
                new MessageFactory<StackStartEvent>(), new StackStartEventConverter());
        flow.initialize(flowId);
        return flow;
    }

    @Override
    public List<StackStartEvent> getFlowTriggerEvents() {
        return Collections.singletonList(START_EVENT);
    }

    @Override
    public StackStartEvent[] getEvents() {
        return StackStartEvent.values();
    }

    @Override
    protected MachineConfiguration<StackStartState, StackStartEvent> getStateMachineConfiguration() {
        StateMachineConfigurationBuilder<StackStartState, StackStartEvent> configurationBuilder =
                new StateMachineConfigurationBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineStateBuilder<StackStartState, StackStartEvent> stateBuilder =
                new StateMachineStateBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineTransitionBuilder<StackStartState, StackStartEvent> transitionBuilder =
                new StateMachineTransitionBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineListener<StackStartState, StackStartEvent> listener =
                new StateMachineListenerAdapter<StackStartState, StackStartEvent>() {
                    @Override
                    public void stateChanged(State<StackStartState, StackStartEvent> from, State<StackStartState, StackStartEvent> to) {
                        LOGGER.info("StackStartFlowConfig changed from {} to {}", from, to);
                    }
                };
        return new MachineConfiguration<>(configurationBuilder, stateBuilder, transitionBuilder, listener, new SyncTaskExecutor());
    }

    @Override
    protected List<Transition<StackStartState, StackStartEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<StackStartState, StackStartEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
