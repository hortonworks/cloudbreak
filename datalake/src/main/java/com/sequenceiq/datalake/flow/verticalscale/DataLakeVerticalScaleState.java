package com.sequenceiq.datalake.flow.verticalscale;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DataLakeVerticalScaleState implements FlowState {

    INIT_STATE,
    VERTICAL_SCALING_DATALAKE_VALIDATION_STATE,
    VERTICAL_SCALING_DATALAKE_STATE,
    VERTICAL_SCALING_DATALAKE_FINISHED_STATE,
    VERTICAL_SCALING_DATALAKE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
