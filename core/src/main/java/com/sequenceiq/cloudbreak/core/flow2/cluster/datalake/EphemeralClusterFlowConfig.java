package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.EphemeralClusterEvent.EPHEMERAL_CLUSTER_FAILURE_HANDLED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.EphemeralClusterEvent.EPHEMERAL_CLUSTER_FLOW_FINISHED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.EphemeralClusterEvent.EPHEMERAL_CLUSTER_UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.EphemeralClusterEvent.EPHEMERAL_CLUSTER_UPDATE_FINISHED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.EphemeralClusterEvent.EPHEMERAL_CLUSTER_UPDATE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.EphemeralClusterState.EPHEMERAL_CLUSTER_UPDATE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.EphemeralClusterState.EPHEMERAL_CLUSTER_UPDATE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.EphemeralClusterState.EPHEMERAL_CLUSTER_UPDATE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.EphemeralClusterState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.EphemeralClusterState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class EphemeralClusterFlowConfig extends AbstractFlowConfiguration<EphemeralClusterState, EphemeralClusterEvent> {

    private static final List<Transition<EphemeralClusterState, EphemeralClusterEvent>> TRANSITIONS =
            new Builder<EphemeralClusterState, EphemeralClusterEvent>()
                    .defaultFailureEvent(EPHEMERAL_CLUSTER_UPDATE_FAILED)
                    .from(INIT_STATE).to(EPHEMERAL_CLUSTER_UPDATE_STATE).event(EPHEMERAL_CLUSTER_UPDATE_TRIGGER_EVENT).noFailureEvent()
                    .from(EPHEMERAL_CLUSTER_UPDATE_STATE).to(EPHEMERAL_CLUSTER_UPDATE_FINISHED_STATE).event(EPHEMERAL_CLUSTER_UPDATE_FINISHED)
                    .failureEvent(EPHEMERAL_CLUSTER_UPDATE_FAILED)
                    .from(EPHEMERAL_CLUSTER_UPDATE_FINISHED_STATE).to(FINAL_STATE).event(EPHEMERAL_CLUSTER_FLOW_FINISHED).defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<EphemeralClusterState, EphemeralClusterEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            EPHEMERAL_CLUSTER_UPDATE_FAILED_STATE, EPHEMERAL_CLUSTER_FAILURE_HANDLED);

    public EphemeralClusterFlowConfig() {
        super(EphemeralClusterState.class, EphemeralClusterEvent.class);
    }

    @Override
    protected List<Transition<EphemeralClusterState, EphemeralClusterEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<EphemeralClusterState, EphemeralClusterEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public EphemeralClusterEvent[] getEvents() {
        return EphemeralClusterEvent.values();
    }

    @Override
    public EphemeralClusterEvent[] getInitEvents() {
        return new EphemeralClusterEvent[]{EPHEMERAL_CLUSTER_UPDATE_TRIGGER_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Ephemeral cluster";
    }
}
