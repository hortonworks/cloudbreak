package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum PrepareUpgradeState implements FlowState {
    INIT_STATE,
    PREPARE_UPGRADE_FAILURE_CLEANUP_STATE,
    PREPARE_UPGRADE_FAILED_STATE,
    PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_STATE,
    PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_RESULT_STATE,
    PREPARE_UPGRADE_PREPARE_IMAGE_STATE,
    PREPARE_UPGRADE_SET_FALLBACK_IMAGE_STATE,
    PREPARE_UPGRADE_UPDATE_IMAGE_PARAMETER_STATE,
    PREPARE_UPGRADE_CHECK_IMAGE_STATE,
    PREPARE_UPGRADE_LB_CONFIGURATION_STATE,
    PREPARE_UPGRADE_LB_PROVISION_STATE,
    PREPARE_UPGRADE_METADATA_COLLECTION_STATE,
    PREPARE_UPGRADE_LB_DELETION_STATE,
    PREPARE_UPGRADE_LB_DB_CLEANUP_STATE,
    PREPARE_UPGRADE_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
