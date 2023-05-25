package com.sequenceiq.flow.rotation.status;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum SecretRotationStatusChangeState implements FlowState {

    INIT_STATE,
    SECRET_ROTATION_STATUS_CHANGE_STARTED_STATE,
    SECRET_ROTATION_STATUS_CHANGE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return DefaultRestartAction.class;
    }
}
