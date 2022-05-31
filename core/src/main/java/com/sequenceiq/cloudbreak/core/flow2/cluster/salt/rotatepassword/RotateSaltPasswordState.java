package com.sequenceiq.cloudbreak.core.flow2.cluster.salt.rotatepassword;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum RotateSaltPasswordState implements FlowState {
    INIT_STATE,

    ROTATE_SALT_PASSWORD_FAILED_STATE,

    ROTATE_SALT_PASSWORD_STATE,
    ROTATE_SALT_PASSWORD_SUCCESS_STATE,

    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
