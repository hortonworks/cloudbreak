package com.sequenceiq.datalake.flow.salt.rotatepassword;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum RotateSaltPasswordTrackerState implements FlowState {
    INIT_STATE,

    ROTATE_SALT_PASSWORD_WAITING_STATE,

    ROTATE_SALT_PASSWORD_SUCCESS_STATE,
    ROTATE_SALT_PASSWORD_FAILED_STATE,

    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
