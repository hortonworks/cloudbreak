package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;


import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.CLUSTER_TERMINATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.TERMINATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.TERMINATION_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.TERMINATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.TERMINATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.TERMINATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.TERMINATION_STATE;

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
public class ClusterTerminationFlowConfig extends AbstractFlowConfiguration<ClusterTerminationState, ClusterTerminationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTerminationFlowConfig.class);
    private static final List<Transition<ClusterTerminationState, ClusterTerminationEvent>> TRANSITIONS = Arrays.asList(
            new Transition<>(INIT_STATE, TERMINATION_STATE, TERMINATION_EVENT),
            new Transition<>(TERMINATION_STATE, TERMINATION_FINISHED_STATE, TERMINATION_FINISHED_EVENT)
    );
    private static final FlowEdgeConfig<ClusterTerminationState, ClusterTerminationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, TERMINATION_FINISHED_STATE, TERMINATION_FINALIZED_EVENT, TERMINATION_FAILED_STATE,
                    CLUSTER_TERMINATION_FAIL_HANDLED_EVENT);


    @Override
    public Flow<ClusterTerminationState, ClusterTerminationEvent> createFlow(String flowId) {
        Flow<ClusterTerminationState, ClusterTerminationEvent> flow = new Flow<>(getStateMachineFactory().getStateMachine(),
                new MessageFactory<ClusterTerminationEvent>(), new ClusterTerminationEventConverter());
        flow.initialize(flowId);
        return flow;
    }

    @Override
    protected MachineConfiguration<ClusterTerminationState, ClusterTerminationEvent> getStateMachineConfiguration() {
        StateMachineConfigurationBuilder<ClusterTerminationState, ClusterTerminationEvent> configurationBuilder =
                new StateMachineConfigurationBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineStateBuilder<ClusterTerminationState, ClusterTerminationEvent> stateBuilder =
                new StateMachineStateBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineTransitionBuilder<ClusterTerminationState, ClusterTerminationEvent> transitionBuilder =
                new StateMachineTransitionBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineListener<ClusterTerminationState, ClusterTerminationEvent> listener =
                new StateMachineListenerAdapter<ClusterTerminationState, ClusterTerminationEvent>() {
                    @Override
                    public void stateChanged(State<ClusterTerminationState, ClusterTerminationEvent> from, State<ClusterTerminationState,
                            ClusterTerminationEvent> to) {
                        LOGGER.info("ClusterTerminationFlow changed from {} to {}", from, to);
                    }
                };
        return new MachineConfiguration<>(configurationBuilder, stateBuilder, transitionBuilder, listener, new SyncTaskExecutor());
    }

    @Override
    protected List<Transition<ClusterTerminationState, ClusterTerminationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ClusterTerminationState, ClusterTerminationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public List<ClusterTerminationEvent> getFlowTriggerEvents() {
        return Collections.singletonList(ClusterTerminationEvent.TERMINATION_EVENT);
    }

    @Override
    public ClusterTerminationEvent[] getEvents() {
        return ClusterTerminationEvent.values();
    }

}
