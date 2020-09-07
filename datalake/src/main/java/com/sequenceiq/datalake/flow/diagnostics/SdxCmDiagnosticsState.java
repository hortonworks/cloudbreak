package com.sequenceiq.datalake.flow.diagnostics;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum SdxCmDiagnosticsState  implements FlowState {
    INIT_STATE,
    CM_DIAGNOSTICS_COLLECTION_START_STATE,
    CM_DIAGNOSTICS_COLLECTION_IN_PROGRESS_STATE,
    CM_DIAGNOSTICS_COLLECTION_FAILED_STATE,
    CM_DIAGNOSTICS_COLLECTION_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    SdxCmDiagnosticsState() {
    }

    SdxCmDiagnosticsState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
