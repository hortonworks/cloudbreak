package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum ClusterUpgradePreparationState implements FlowState {

    INIT_STATE,
    CLUSTER_UPGRADE_PREPARATION_INIT_STATE,
    CLUSTER_UPGRADE_PREPARATION_DOWNLOAD_CM_PACKAGES_STATE,
    CLUSTER_UPGRADE_PREPARATION_PARCEL_DOWNLOAD_STATE,
    CLUSTER_UPGRADE_PREPARATION_PARCEL_DISTRIBUTION_STATE,
    CLUSTER_UPGRADE_PREPARATION_DOWNLOAD_CSD_PACKAGES_STATE,
    CLUSTER_UPGRADE_PREPARATION_FINISHED_STATE,
    CLUSTER_UPGRADE_PREPARATION_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
