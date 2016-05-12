package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;


import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.CLUSTER_TERMINATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.TERMINATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.TERMINATION_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.TERMINATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.TERMINATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.TERMINATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.TERMINATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.TERMINATION_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class ClusterTerminationFlowConfig extends AbstractFlowConfiguration<ClusterTerminationState, ClusterTerminationEvent> {

    private static final List<Transition<ClusterTerminationState, ClusterTerminationEvent>> TRANSITIONS =
            new Transition.Builder<ClusterTerminationState, ClusterTerminationEvent>()
                    .defaultFailureEvent(TERMINATION_FAILED_EVENT)
                    .from(INIT_STATE).to(TERMINATION_STATE).event(TERMINATION_EVENT).noFailureEvent()
                    .from(TERMINATION_STATE).to(TERMINATION_FINISHED_STATE).event(TERMINATION_FINISHED_EVENT).defaultFailureEvent()
                    .from(TERMINATION_FINISHED_STATE).to(FINAL_STATE).event(TERMINATION_FINALIZED_EVENT).defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<ClusterTerminationState, ClusterTerminationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, TERMINATION_FAILED_STATE, CLUSTER_TERMINATION_FAIL_HANDLED_EVENT);

    public ClusterTerminationFlowConfig() {
        super(ClusterTerminationState.class, ClusterTerminationEvent.class);
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
    public ClusterTerminationEvent[] getEvents() {
        return ClusterTerminationEvent.values();
    }

}
