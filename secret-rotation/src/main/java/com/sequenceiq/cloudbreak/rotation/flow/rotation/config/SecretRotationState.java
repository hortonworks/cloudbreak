package com.sequenceiq.cloudbreak.rotation.flow.rotation.config;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum SecretRotationState implements FlowState {

    INIT_STATE,
    PRE_VALIDATE_ROTATION_STATE,
    EXECUTE_ROTATION_STATE,
    FINALIZE_ROTATION_STATE,
    ROLLBACK_ROTATION_STATE,
    ROTATION_DEFAULT_FAILURE_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return DefaultRestartAction.class;
    }
}
