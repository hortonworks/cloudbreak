package com.sequenceiq.datalake.flow.dr.datalakeinfo;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum SubmitDatalakeDataInfoState implements FlowState {
    INIT_STATE,
    SUBMIT_DATALAKE_DATA_INFO_IN_PROGRESS_STATE,
    SUBMIT_DATALAKE_DATA_INFO_FAILED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    SubmitDatalakeDataInfoState() {
    }

    SubmitDatalakeDataInfoState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends DefaultRestartAction> restartAction() {
        return restartAction;
    }
}
