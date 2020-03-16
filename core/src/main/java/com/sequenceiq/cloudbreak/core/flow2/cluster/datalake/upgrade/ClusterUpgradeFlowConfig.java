package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_MANAGER_UPGRADE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_MANAGER_UPGRADE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeState.CLUSTER_MANAGER_UPGRADE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeState.CLUSTER_UPGRADE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeState.CLUSTER_UPGRADE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeState.CLUSTER_UPGRADE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ClusterUpgradeFlowConfig extends AbstractFlowConfiguration<ClusterUpgradeState, ClusterUpgradeEvent>
    implements RetryableFlowConfiguration<ClusterUpgradeEvent> {

    private static final List<Transition<ClusterUpgradeState, ClusterUpgradeEvent>> TRANSITIONS =
            new Builder<ClusterUpgradeState, ClusterUpgradeEvent>()
                    .defaultFailureEvent(CLUSTER_UPGRADE_FAILED_EVENT)

                    .from(INIT_STATE).to(CLUSTER_MANAGER_UPGRADE_STATE).event(CLUSTER_MANAGER_UPGRADE_EVENT)
                    .defaultFailureEvent()

                    .from(INIT_STATE).to(CLUSTER_UPGRADE_STATE).event(CLUSTER_MANAGER_UPGRADE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(CLUSTER_MANAGER_UPGRADE_STATE).to(CLUSTER_UPGRADE_STATE).event(CLUSTER_MANAGER_UPGRADE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(CLUSTER_UPGRADE_STATE).to(CLUSTER_UPGRADE_FINISHED_STATE).event(CLUSTER_UPGRADE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(CLUSTER_UPGRADE_FINISHED_STATE).to(FINAL_STATE).event(CLUSTER_UPGRADE_FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<ClusterUpgradeState, ClusterUpgradeEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            CLUSTER_UPGRADE_FAILED_STATE, CLUSTER_UPGRADE_FAIL_HANDLED_EVENT);

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
                CLUSTER_MANAGER_UPGRADE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Upgrade Datalake cluster";
    }

    @Override
    public ClusterUpgradeEvent getFailHandledEvent() {
        return CLUSTER_UPGRADE_FAIL_HANDLED_EVENT;
    }
}
