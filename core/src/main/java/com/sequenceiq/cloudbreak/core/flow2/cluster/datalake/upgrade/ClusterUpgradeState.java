package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum ClusterUpgradeState implements FlowState {
    INIT_STATE,
    CLUSTER_UPGRADE_FAILED_STATE,
    CLUSTER_UPGRADE_INIT_STATE,
    CLUSTER_MANAGER_UPGRADE_STATE,
    CLUSTER_UPGRADE_STATE,
    CLUSTER_UPGRADE_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
