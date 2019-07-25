package com.sequenceiq.datalake.flow.delete;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum SdxDeleteState implements FlowState {
    INIT_STATE,
    SDX_DELETION_START_STATE,
    SDX_STACK_DELETION_IN_PROGRESS_STATE,
    SDX_DELETION_WAIT_RDS_STATE,
    SDX_DELETION_FAILED_STATE,
    SDX_DELETION_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = DefaultRestartAction.class;

    SdxDeleteState() {
    }

    SdxDeleteState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
