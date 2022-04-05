package com.sequenceiq.datalake.flow.refresh;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DatahubRefreshFlowState implements FlowState {

    INIT_STATE,
    DATAHUB_REFRESH_START_STATE,
    DATAHUB_REFRESH_IN_PROGRESS_STATE,
    DATAHUB_REFRESH_FINISHED_STATE,
    DATAHUB_REFRESH_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
