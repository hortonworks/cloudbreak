package com.sequenceiq.datalake.flow.detach;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum SdxDetachState implements FlowState {
    INIT_STATE,
    SDX_DETACH_CLUSTER_STATE,
    SDX_DETACH_FAILED_STATE,
    SDX_DETACH_STACK_STATE,
    SDX_DETACH_STACK_FAILED_STATE,
    SDX_DETACH_EXTERNAL_DB_STATE,
    SDX_DETACH_EXTERNAL_DB_FAILED_STATE,
    SDX_ATTACH_NEW_CLUSTER_STATE,
    SDX_ATTACH_NEW_CLUSTER_FAILED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    SdxDetachState() {
    }

    SdxDetachState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }

}
