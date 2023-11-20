package com.sequenceiq.cloudbreak.rotation.flow.subrotation.config;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum SecretSubRotationState implements FlowState {

    INIT_STATE,
    EXECUTE_SUB_ROTATION_STATE,
    SUB_ROTATION_FAILURE_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return DefaultRestartAction.class;
    }
}
