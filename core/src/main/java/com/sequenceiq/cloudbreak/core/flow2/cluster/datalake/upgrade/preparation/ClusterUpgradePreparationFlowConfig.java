package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationState.CLUSTER_UPGRADE_PREPARATION_DOWNLOAD_CM_PACKAGES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationState.CLUSTER_UPGRADE_PREPARATION_DOWNLOAD_CSD_PACKAGES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationState.CLUSTER_UPGRADE_PREPARATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationState.CLUSTER_UPGRADE_PREPARATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationState.CLUSTER_UPGRADE_PREPARATION_INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationState.CLUSTER_UPGRADE_PREPARATION_PARCEL_DISTRIBUTION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationState.CLUSTER_UPGRADE_PREPARATION_PARCEL_DOWNLOAD_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.FINALIZE_CLUSTER_UPGRADE_PREPARATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.FINISH_CLUSTER_UPGRADE_PREPARATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.HANDLED_FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.START_CLUSTER_UPGRADE_CM_PACKAGE_DOWNLOAD_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.START_CLUSTER_UPGRADE_CSD_PACKAGE_DOWNLOAD_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.START_CLUSTER_UPGRADE_PARCEL_DISTRIBUTION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.START_CLUSTER_UPGRADE_PARCEL_DOWNLOAD_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.START_CLUSTER_UPGRADE_PREPARATION_INIT_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ClusterUpgradePreparationFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<ClusterUpgradePreparationState, ClusterUpgradePreparationStateSelectors>
        implements RetryableFlowConfiguration<ClusterUpgradePreparationStateSelectors> {

    private static final List<Transition<ClusterUpgradePreparationState, ClusterUpgradePreparationStateSelectors>> TRANSITIONS =
            new Transition.Builder<ClusterUpgradePreparationState, ClusterUpgradePreparationStateSelectors>()
                    .defaultFailureEvent(FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT)

                    .from(INIT_STATE).to(CLUSTER_UPGRADE_PREPARATION_INIT_STATE)
                    .event(START_CLUSTER_UPGRADE_PREPARATION_INIT_EVENT)
                    .defaultFailureEvent()

                    .from(CLUSTER_UPGRADE_PREPARATION_INIT_STATE).to(CLUSTER_UPGRADE_PREPARATION_DOWNLOAD_CM_PACKAGES_STATE)
                    .event(START_CLUSTER_UPGRADE_CM_PACKAGE_DOWNLOAD_EVENT)
                    .defaultFailureEvent()

                    .from(CLUSTER_UPGRADE_PREPARATION_DOWNLOAD_CM_PACKAGES_STATE).to(CLUSTER_UPGRADE_PREPARATION_PARCEL_DOWNLOAD_STATE)
                    .event(START_CLUSTER_UPGRADE_PARCEL_DOWNLOAD_EVENT)
                    .defaultFailureEvent()

                    .from(CLUSTER_UPGRADE_PREPARATION_PARCEL_DOWNLOAD_STATE).to(CLUSTER_UPGRADE_PREPARATION_PARCEL_DISTRIBUTION_STATE)
                    .event(START_CLUSTER_UPGRADE_PARCEL_DISTRIBUTION_EVENT)
                    .defaultFailureEvent()

                    .from(CLUSTER_UPGRADE_PREPARATION_PARCEL_DISTRIBUTION_STATE).to(CLUSTER_UPGRADE_PREPARATION_DOWNLOAD_CSD_PACKAGES_STATE)
                    .event(START_CLUSTER_UPGRADE_CSD_PACKAGE_DOWNLOAD_EVENT)
                    .defaultFailureEvent()

                    .from(CLUSTER_UPGRADE_PREPARATION_DOWNLOAD_CSD_PACKAGES_STATE).to(CLUSTER_UPGRADE_PREPARATION_FINISHED_STATE)
                    .event(FINISH_CLUSTER_UPGRADE_PREPARATION_EVENT)
                    .defaultFailureEvent()

                    .from(CLUSTER_UPGRADE_PREPARATION_FINISHED_STATE).to(FINAL_STATE)
                    .event(FINALIZE_CLUSTER_UPGRADE_PREPARATION_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<ClusterUpgradePreparationState, ClusterUpgradePreparationStateSelectors> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE,
            FINAL_STATE, CLUSTER_UPGRADE_PREPARATION_FAILED_STATE, HANDLED_FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT);

    protected ClusterUpgradePreparationFlowConfig() {
        super(ClusterUpgradePreparationState.class, ClusterUpgradePreparationStateSelectors.class);
    }

    @Override
    protected List<Transition<ClusterUpgradePreparationState, ClusterUpgradePreparationStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ClusterUpgradePreparationState, ClusterUpgradePreparationStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ClusterUpgradePreparationStateSelectors[] getEvents() {
        return ClusterUpgradePreparationStateSelectors.values();
    }

    @Override
    public ClusterUpgradePreparationStateSelectors[] getInitEvents() {
        return new ClusterUpgradePreparationStateSelectors[] {
                START_CLUSTER_UPGRADE_PREPARATION_INIT_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Cluster upgrade preparation flow";
    }

    @Override
    public ClusterUpgradePreparationStateSelectors getRetryableEvent() {
        return ClusterUpgradePreparationStateSelectors.HANDLED_FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT;
    }
}
