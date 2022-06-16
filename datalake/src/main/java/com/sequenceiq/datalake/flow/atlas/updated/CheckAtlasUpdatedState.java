package com.sequenceiq.datalake.flow.atlas.updated;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum CheckAtlasUpdatedState implements FlowState {
    INIT_STATE,
    CHECK_ATLAS_UPDATED_STATE,
    CHECK_ATLAS_UPDATED_FAILED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    CheckAtlasUpdatedState() {
    }

    CheckAtlasUpdatedState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
