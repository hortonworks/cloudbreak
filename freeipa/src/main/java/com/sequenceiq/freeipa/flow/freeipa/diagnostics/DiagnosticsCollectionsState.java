package com.sequenceiq.freeipa.flow.freeipa.diagnostics;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum DiagnosticsCollectionsState implements FlowState {
    INIT_STATE,
    DIAGNOSTICS_SALT_VALIDATION_STATE,
    DIAGNOSTICS_SALT_PILLAR_UPDATE_STATE,
    DIAGNOSTICS_SALT_STATE_UPDATE_STATE,
    DIAGNOSTICS_PREFLIGHT_CHECK_STATE,
    DIAGNOSTICS_INIT_STATE,
    DIAGNOSTICS_UPGRADE_STATE,
    DIAGNOSTICS_VM_PREFLIGHT_CHECK_STATE,
    DIAGNOSTICS_ENSURE_MACHINE_USER_STATE,
    DIAGNOSTICS_COLLECTION_STATE,
    DIAGNOSTICS_UPLOAD_STATE,
    DIAGNOSTICS_CLEANUP_STATE,
    DIAGNOSTICS_COLLECTION_FINISHED_STATE,
    DIAGNOSTICS_COLLECTION_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
