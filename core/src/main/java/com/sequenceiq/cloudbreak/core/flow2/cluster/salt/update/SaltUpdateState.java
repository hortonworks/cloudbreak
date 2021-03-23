package com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum SaltUpdateState implements FlowState {
    INIT_STATE,
    UPDATE_SALT_STATE_FILES_STATE,
    UPLOAD_RECIPES_FOR_SU_STATE,
    RECONFIGURE_KEYTABS_FOR_SU_STATE,
    RUN_HIGHSTATE_STATE,
    SALT_UPDATE_FINISHED_STATE,
    FINAL_STATE,
    SALT_UPDATE_FAILED_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
