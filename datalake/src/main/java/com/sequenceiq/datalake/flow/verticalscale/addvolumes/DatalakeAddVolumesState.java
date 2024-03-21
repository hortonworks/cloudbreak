package com.sequenceiq.datalake.flow.verticalscale.addvolumes;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DatalakeAddVolumesState implements FlowState {

    INIT_STATE,
    DATALAKE_ADD_VOLUMES_STATE,
    DATALAKE_ADD_VOLUMES_FINISHED_STATE,
    DATALAKE_ADD_VOLUMES_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}