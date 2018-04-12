package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.COLLECT_CANDIDATES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.COLLECT_CANDIDATES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.CLUSTER_DOWNSCALE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.COLLECT_CANDIDATES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.DECOMMISSION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.UPDATE_INSTANCE_METADATA_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class ClusterDownscaleFlowConfig extends AbstractFlowConfiguration<ClusterDownscaleState, ClusterDownscaleEvent> {
    private static final List<Transition<ClusterDownscaleState, ClusterDownscaleEvent>> TRANSITIONS =
            new Builder<ClusterDownscaleState, ClusterDownscaleEvent>()
                    .defaultFailureEvent(FAILURE_EVENT)
                    .from(INIT_STATE).to(COLLECT_CANDIDATES_STATE).event(DECOMMISSION_EVENT).noFailureEvent()
                    .from(COLLECT_CANDIDATES_STATE).to(DECOMMISSION_STATE).event(COLLECT_CANDIDATES_FINISHED_EVENT)
                        .failureEvent(COLLECT_CANDIDATES_FAILED_EVENT)
                    .from(DECOMMISSION_STATE).to(UPDATE_INSTANCE_METADATA_STATE).event(DECOMMISSION_FINISHED_EVENT)
                        .failureEvent(DECOMMISSION_FAILED_EVENT)
                    .from(UPDATE_INSTANCE_METADATA_STATE).to(FINAL_STATE).event(FINALIZED_EVENT).defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<ClusterDownscaleState, ClusterDownscaleEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            CLUSTER_DOWNSCALE_FAILED_STATE, FAIL_HANDLED_EVENT);

    public ClusterDownscaleFlowConfig() {
        super(ClusterDownscaleState.class, ClusterDownscaleEvent.class);
    }

    @Override
    protected List<Transition<ClusterDownscaleState, ClusterDownscaleEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ClusterDownscaleState, ClusterDownscaleEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ClusterDownscaleEvent[] getEvents() {
        return ClusterDownscaleEvent.values();
    }

    @Override
    public ClusterDownscaleEvent[] getInitEvents() {
        return new ClusterDownscaleEvent[] { DECOMMISSION_EVENT };
    }
}
