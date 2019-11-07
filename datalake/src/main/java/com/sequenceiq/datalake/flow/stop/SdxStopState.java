package com.sequenceiq.datalake.flow.stop;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum SdxStopState implements FlowState {

    INIT_STATE,
    SDX_STOP_START_STATE,
    SDX_STOP_IN_PROGRESS_STATE,
    SDX_STOP_ALL_DATAHUBS_STATE,
    SDX_STOP_FAILED_STATE,
    SDX_STOP_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    SdxStopState() {
    }

    SdxStopState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }

}
