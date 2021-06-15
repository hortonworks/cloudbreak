package com.sequenceiq.freeipa.flow.freeipa.salt.update;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum SaltUpdateState implements FlowState {
    INIT_STATE,
    UPDATE_SALT_STATE_FILES_STATE,
    UPDATE_ORCHESTRATOR_CONFIG_STATE,
    RUN_HIGHSTATE_STATE,
    SALT_UPDATE_FINISHED_STATE,
    FINAL_STATE,
    SALT_UPDATE_FAILED_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
