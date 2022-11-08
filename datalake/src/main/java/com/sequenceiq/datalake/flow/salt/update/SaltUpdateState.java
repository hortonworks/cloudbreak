package com.sequenceiq.datalake.flow.salt.update;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum SaltUpdateState implements FlowState {

    INIT_STATE,
    SALT_UPDATE_STATE,
    SALT_UPDATE_WAIT_STATE,
    SALT_UPDATE_FINISHED_STATE,
    SALT_UPDATE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
