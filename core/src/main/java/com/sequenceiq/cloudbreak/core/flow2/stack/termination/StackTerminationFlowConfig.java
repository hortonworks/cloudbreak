package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.FORCE_TERMINATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.STACK_TERMINATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.TERMINATION_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.TERMINATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.TERMINATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.FORCE_TERMINATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.TERMINATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.TERMINATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.TERMINATION_STATE;

import java.util.Arrays;
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
public class StackTerminationFlowConfig extends AbstractFlowConfiguration<StackTerminationState, StackTerminationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationFlowConfig.class);
    private static final List<Transition<StackTerminationState, StackTerminationEvent>> TRANSITIONS = Arrays.asList(
            new Transition<>(INIT_STATE, TERMINATION_STATE, TERMINATION_EVENT),
            new Transition<>(INIT_STATE, FORCE_TERMINATION_STATE, FORCE_TERMINATION_EVENT),
            new Transition<>(TERMINATION_STATE, TERMINATION_FINISHED_STATE, TERMINATION_FINISHED_EVENT)
    );
    private static final FlowEdgeConfig<StackTerminationState, StackTerminationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, TERMINATION_FINISHED_STATE, TERMINATION_FINALIZED_EVENT, TERMINATION_FAILED_STATE,
                    STACK_TERMINATION_FAIL_HANDLED_EVENT);

    @Override
    public Flow<StackTerminationState, StackTerminationEvent> createFlow(String flowId) {
        Flow<StackTerminationState, StackTerminationEvent> flow = new Flow<>(getStateMachineFactory().getStateMachine(),
                new MessageFactory<StackTerminationEvent>(), new StackTerminationEventConverter());
        flow.initialize(flowId);
        return flow;
    }

    @Override
    protected MachineConfiguration<StackTerminationState, StackTerminationEvent> getStateMachineConfiguration() {
        StateMachineConfigurationBuilder<StackTerminationState, StackTerminationEvent> configurationBuilder =
                new StateMachineConfigurationBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineStateBuilder<StackTerminationState, StackTerminationEvent> stateBuilder =
                new StateMachineStateBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineTransitionBuilder<StackTerminationState, StackTerminationEvent> transitionBuilder =
                new StateMachineTransitionBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineListener<StackTerminationState, StackTerminationEvent> listener =
                new StateMachineListenerAdapter<StackTerminationState, StackTerminationEvent>() {
                    @Override
                    public void stateChanged(State<StackTerminationState, StackTerminationEvent> from, State<StackTerminationState, StackTerminationEvent> to) {
                        LOGGER.info("StackTerminationFlow changed from {} to {}", from, to);
                    }
                };
        return new MachineConfiguration<>(configurationBuilder, stateBuilder, transitionBuilder, listener, new SyncTaskExecutor());
    }

    @Override
    protected List<Transition<StackTerminationState, StackTerminationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<StackTerminationState, StackTerminationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public List<StackTerminationEvent> getFlowTriggerEvents() {
        return Arrays.asList(TERMINATION_EVENT, FORCE_TERMINATION_EVENT);
    }

    @Override
    public StackTerminationEvent[] getEvents() {
        return StackTerminationEvent.values();
    }
}
