package com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum MaintenanceModeValidationState implements FlowState {

    INIT_STATE,
    FETCH_STACK_REPO_STATE,
    VALIDATE_STACK_REPO_INFO_STATE,
    VALIDATE_AMBARI_REPO_INFO_STATE,
    VALIDATE_IMAGE_COMPATIBILITY_STATE,
    VALIDATION_FINISHED_STATE,
    VALIDATION_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
