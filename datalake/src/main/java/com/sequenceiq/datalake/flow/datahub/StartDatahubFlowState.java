package com.sequenceiq.datalake.flow.datahub;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum StartDatahubFlowState implements FlowState {
    INIT_STATE,
    START_DATAHUB_STATE,
    START_DATAHUB_FINISHED_STATE,
    START_DATAHUB_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }

}
