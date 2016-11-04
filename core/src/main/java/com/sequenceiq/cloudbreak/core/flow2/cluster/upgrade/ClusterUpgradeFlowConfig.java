package com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_FINISHED_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade.ClusterUpgradeEvent.FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade.ClusterUpgradeEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade.ClusterUpgradeEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade.ClusterUpgradeState.CLUSTER_UPGRADE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade.ClusterUpgradeState.CLUSTER_UPGRADE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade.ClusterUpgradeState.CLUSTER_UPGRADE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade.ClusterUpgradeState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade.ClusterUpgradeState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class ClusterUpgradeFlowConfig extends AbstractFlowConfiguration<ClusterUpgradeState, ClusterUpgradeEvent> {
    private static final List<Transition<ClusterUpgradeState, ClusterUpgradeEvent>> TRANSITIONS =
            new Transition.Builder<ClusterUpgradeState, ClusterUpgradeEvent>()
                    .from(INIT_STATE).to(CLUSTER_UPGRADE_STATE).event(CLUSTER_UPGRADE_EVENT)
                    .noFailureEvent()

                    .from(CLUSTER_UPGRADE_STATE).to(CLUSTER_UPGRADE_FINISHED_STATE).event(CLUSTER_UPGRADE_FINISHED_EVENT)
                    .failureEvent(CLUSTER_UPGRADE_FINISHED_FAILURE_EVENT)

                    .from(CLUSTER_UPGRADE_FINISHED_STATE).to(FINAL_STATE).event(FINALIZED_EVENT)
                    .failureEvent(FAILURE_EVENT)

                    .build();

    private static final FlowEdgeConfig<ClusterUpgradeState, ClusterUpgradeEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            CLUSTER_UPGRADE_FAILED_STATE, FAIL_HANDLED_EVENT);

    public ClusterUpgradeFlowConfig() {
        super(ClusterUpgradeState.class, ClusterUpgradeEvent.class);
    }

    @Override
    protected List<Transition<ClusterUpgradeState, ClusterUpgradeEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ClusterUpgradeState, ClusterUpgradeEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ClusterUpgradeEvent[] getEvents() {
        return ClusterUpgradeEvent.values();
    }

    @Override
    public ClusterUpgradeEvent[] getInitEvents() {
        return new ClusterUpgradeEvent[]{
                CLUSTER_UPGRADE_EVENT
        };
    }
}
