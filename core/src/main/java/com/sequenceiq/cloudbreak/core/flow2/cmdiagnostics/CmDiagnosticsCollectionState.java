package com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum CmDiagnosticsCollectionState implements FlowState {
    INIT_STATE,
    CM_DIAGNOSTICS_INIT_STATE,
    CM_DIAGNOSTICS_COLLECTION_STATE,
    CM_DIAGNOSTICS_UPLOAD_STATE,
    CM_DIAGNOSTICS_CLEANUP_STATE,
    CM_DIAGNOSTICS_COLLECTION_FINISHED_STATE,
    CM_DIAGNOSTICS_COLLECTION_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
