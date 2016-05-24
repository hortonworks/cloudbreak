package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.CLUSTER_START_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.CLUSTER_START_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.CLUSTER_START_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class ClusterStartFlowConfig extends AbstractFlowConfiguration<ClusterStartState, ClusterStartEvent> {
    private static final List<Transition<ClusterStartState, ClusterStartEvent>> TRANSITIONS =
            new Transition.Builder<ClusterStartState, ClusterStartEvent>()
                    .from(INIT_STATE).to(CLUSTER_START_STATE).event(CLUSTER_START_EVENT).noFailureEvent()
                    .from(CLUSTER_START_STATE).to(CLUSTER_START_FINISHED_STATE).event(ClusterStartEvent.CLUSTER_START_FINISHED_EVENT)
                                .failureEvent(ClusterStartEvent.CLUSTER_START_FINISHED_FAILURE_EVENT)
                    .from(CLUSTER_START_FINISHED_STATE).to(FINAL_STATE).event(FINALIZED_EVENT).noFailureEvent()
                    .build();

    private static final FlowEdgeConfig<ClusterStartState, ClusterStartEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            CLUSTER_START_FAILED_STATE, FAIL_HANDLED_EVENT);

    public ClusterStartFlowConfig() {
        super(ClusterStartState.class, ClusterStartEvent.class);
    }

    @Override

    protected List<Transition<ClusterStartState, ClusterStartEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ClusterStartState, ClusterStartEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ClusterStartEvent[] getEvents() {
        return ClusterStartEvent.values();
    }

    @Override
    public ClusterStartEvent[] getInitEvents() {
        return new ClusterStartEvent[] {
                CLUSTER_START_EVENT
        };
    }
}
