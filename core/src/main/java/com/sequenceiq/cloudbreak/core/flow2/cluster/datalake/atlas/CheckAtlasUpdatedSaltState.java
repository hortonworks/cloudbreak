package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.atlas;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum CheckAtlasUpdatedSaltState implements FlowState {
    INIT_STATE,
    CHECK_ATLAS_UPDATED_SALT_STATE,
    CHECK_ATLAS_UPDATED_SALT_SUCCESS_STATE,
    CHECK_ATLAS_UPDATED_SALT_FAILED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    CheckAtlasUpdatedSaltState() {
    }

    CheckAtlasUpdatedSaltState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
