package com.sequenceiq.datalake.flow.datalake.scale;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum DatalakeHorizontalScaleState implements FlowState {

    INIT_STATE,
    DATALAKE_HORIZONTAL_SCALE_VALIDATION_STATE,
    DATALAKE_HORIZONTAL_SCALE_START_STATE,
    DATALAKE_WAIT_FOR_HORIZONTAL_SCALE_STATE,
    DATALAKE_HORIZONTAL_SCALE_SERVICES_RESTART_STATE,
    DATALAKE_HORIZONTAL_SCALE_SERVICES_RESTART_IN_PROGRESS_STATE,
    DATALAKE_HORIZONTAL_SCALE_FINISHED_STATE,
    DATALAKE_HORIZONTAL_SCALE_FAILED_STATE,
    FINAL_STATE;

    private final Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
