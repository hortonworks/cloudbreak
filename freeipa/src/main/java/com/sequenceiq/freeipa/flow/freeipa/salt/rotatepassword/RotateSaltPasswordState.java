package com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum RotateSaltPasswordState implements FlowState {
    INIT_STATE,

    ROTATE_SALT_PASSWORD_STATE,

    ROTATE_SALT_PASSWORD_SUCCESS_STATE,
    ROTATE_SALT_PASSWORD_FAILED_STATE,

    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
