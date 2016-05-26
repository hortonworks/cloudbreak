package com.sequenceiq.cloudbreak.core.flow2.cluster.sync;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent.CLUSTER_SYNC_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent.FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncState.CLUSTER_SYNC_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncState.CLUSTER_SYNC_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class ClusterSyncFlowConfig extends AbstractFlowConfiguration<ClusterSyncState, ClusterSyncEvent> {
    private static final List<Transition<ClusterSyncState, ClusterSyncEvent>> TRANSITIONS =
            new Transition.Builder<ClusterSyncState, ClusterSyncEvent>()
                    .from(INIT_STATE).to(CLUSTER_SYNC_STATE).event(CLUSTER_SYNC_EVENT).noFailureEvent()
                    .from(CLUSTER_SYNC_STATE).to(ClusterSyncState.CLUSTER_SYNC_FINISHED_STATE).event(ClusterSyncEvent.CLUSTER_SYNC_FINISHED_EVENT)
                    .failureEvent(ClusterSyncEvent.CLUSTER_SYNC_FINISHED_FAILURE_EVENT)
                    .from(ClusterSyncState.CLUSTER_SYNC_FINISHED_STATE).to(FINAL_STATE).event(FINALIZED_EVENT).failureEvent(FAILURE_EVENT)
                    .build();

    private static final FlowEdgeConfig<ClusterSyncState, ClusterSyncEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            CLUSTER_SYNC_FAILED_STATE, FAIL_HANDLED_EVENT);

    public ClusterSyncFlowConfig() {
        super(ClusterSyncState.class, ClusterSyncEvent.class);
    }

    @Override
    protected List<Transition<ClusterSyncState, ClusterSyncEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected AbstractFlowConfiguration.FlowEdgeConfig<ClusterSyncState, ClusterSyncEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ClusterSyncEvent[] getEvents() {
        return ClusterSyncEvent.values();
    }

    @Override
    public ClusterSyncEvent[] getInitEvents() {
        return new ClusterSyncEvent[]{
                CLUSTER_SYNC_EVENT
        };
    }
}
