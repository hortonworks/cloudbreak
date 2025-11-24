package com.sequenceiq.cloudbreak.core.flow2.validate.disk.config;

import com.sequenceiq.cloudbreak.core.flow2.restart.InitializeMDCContextRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DiskValidationState implements FlowState {

    INIT_STATE,
    DISK_VALIDATION_STATE,
    DISK_VALIDATION_FAILED_STATE,
    DISK_VALIDATION_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return InitializeMDCContextRestartAction.class;
    }
}
