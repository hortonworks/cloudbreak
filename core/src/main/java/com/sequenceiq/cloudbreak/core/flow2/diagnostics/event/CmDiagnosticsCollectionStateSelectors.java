package com.sequenceiq.cloudbreak.core.flow2.diagnostics.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum CmDiagnosticsCollectionStateSelectors implements FlowEvent {
    START_CM_DIAGNOSTICS_INIT_EVENT,
    START_CM_DIAGNOSTICS_COLLECTION_EVENT,
    START_CM_DIAGNOSTICS_UPLOAD_EVENT,
    START_CM_DIAGNOSTICS_CLEANUP_EVENT,
    FINISH_CM_DIAGNOSTICS_COLLECTION_EVENT,
    FINALIZE_CM_DIAGNOSTICS_COLLECTION_EVENT,
    FAILED_CM_DIAGNOSTICS_COLLECTION_EVENT,
    HANDLED_FAILED_CM_DIAGNOSTICS_COLLECTION_EVENT;

    @Override
    public String event() {
        return name();
    }
}
