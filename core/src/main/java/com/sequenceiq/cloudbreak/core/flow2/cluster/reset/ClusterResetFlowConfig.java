package com.sequenceiq.cloudbreak.core.flow2.cluster.reset;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetEvent.CLUSTER_RESET_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetEvent.CLUSTER_RESET_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetEvent.CLUSTER_RESET_FINISHED_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetEvent.CLUSTER_RESET_START_AMBARI_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetEvent.CLUSTER_RESET_START_AMBARI_FINISHED_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetEvent.FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetState.CLUSTER_RESET_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetState.CLUSTER_RESET_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetState.CLUSTER_RESET_START_AMBARI_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetState.CLUSTER_RESET_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class ClusterResetFlowConfig extends AbstractFlowConfiguration<ClusterResetState, ClusterResetEvent> {
    private static final List<Transition<ClusterResetState, ClusterResetEvent>> TRANSITIONS =
            new Transition.Builder<ClusterResetState, ClusterResetEvent>()
                    .from(INIT_STATE).to(CLUSTER_RESET_STATE).event(CLUSTER_RESET_EVENT).noFailureEvent()
                    .from(CLUSTER_RESET_STATE).to(CLUSTER_RESET_FINISHED_STATE).event(CLUSTER_RESET_FINISHED_EVENT)
                            .failureEvent(CLUSTER_RESET_FINISHED_FAILURE_EVENT)
                    .from(CLUSTER_RESET_FINISHED_STATE).to(CLUSTER_RESET_START_AMBARI_FINISHED_STATE).event(CLUSTER_RESET_START_AMBARI_FINISHED_EVENT)
                            .failureEvent(CLUSTER_RESET_START_AMBARI_FINISHED_FAILURE_EVENT)
                    .from(CLUSTER_RESET_START_AMBARI_FINISHED_STATE).to(FINAL_STATE).event(FINALIZED_EVENT).failureEvent(FAILURE_EVENT)
                    .build();

    private static final FlowEdgeConfig<ClusterResetState, ClusterResetEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            CLUSTER_RESET_FAILED_STATE, FAIL_HANDLED_EVENT);

    public ClusterResetFlowConfig() {
        super(ClusterResetState.class, ClusterResetEvent.class);
    }

    @Override
    protected List<Transition<ClusterResetState, ClusterResetEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected AbstractFlowConfiguration.FlowEdgeConfig<ClusterResetState, ClusterResetEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ClusterResetEvent[] getEvents() {
        return ClusterResetEvent.values();
    }

    @Override
    public ClusterResetEvent[] getInitEvents() {
        return new ClusterResetEvent[]{
                CLUSTER_RESET_EVENT
        };
    }
}
