package com.sequenceiq.datalake.flow.repair;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum SdxRepairState implements FlowState {

    INIT_STATE,
    SDX_REPAIR_START_STATE,
    SDX_REPAIR_IN_PROGRESS_STATE,
    SDX_REPAIR_COULD_NOT_START_STATE,
    SDX_REPAIR_FAILED_STATE,
    SDX_REPAIR_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    SdxRepairState() {
    }

    SdxRepairState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }

}
