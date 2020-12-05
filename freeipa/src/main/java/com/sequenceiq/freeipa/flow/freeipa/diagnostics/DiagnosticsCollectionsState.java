package com.sequenceiq.freeipa.flow.freeipa.diagnostics;

import com.sequenceiq.flow.core.FlowState;

public enum DiagnosticsCollectionsState implements FlowState {
    INIT_STATE,
    DIAGNOSTICS_INIT_STATE,
    DIAGNOSTICS_ENSURE_MACHINE_USER_STATE,
    DIAGNOSTICS_COLLECTION_STATE,
    DIAGNOSTICS_UPLOAD_STATE,
    DIAGNOSTICS_CLEANUP_STATE,
    DIAGNOSTICS_COLLECTION_FINISHED_STATE,
    DIAGNOSTICS_COLLECTION_FAILED_STATE,
    FINAL_STATE
}
