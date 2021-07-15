package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum ClusterUpgradeValidationState implements FlowState {

    INIT_STATE,
    CLUSTER_UPGRADE_VALIDATION_INIT_STATE,
    CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_STATE,
    CLUSTER_UPGRADE_SERVICE_VALIDATION_STATE,
    CLUSTER_UPGRADE_IMAGE_VALIDATION_STATE,
    CLUSTER_UPGRADE_CLOUDPROVIDER_CHECK_UPDATE_STATE,
    CLUSTER_UPGRADE_VALIDATION_FINISHED_STATE,
    CLUSTER_UPGRADE_VALIDATION_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
