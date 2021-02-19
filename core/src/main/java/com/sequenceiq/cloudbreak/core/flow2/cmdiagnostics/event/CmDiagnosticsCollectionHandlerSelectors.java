package com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum CmDiagnosticsCollectionHandlerSelectors implements FlowEvent {
    INIT_CM_DIAGNOSTICS_EVENT,
    COLLECT_CM_DIAGNOSTICS_EVENT,
    UPLOAD_CM_DIAGNOSTICS_EVENT,
    CLEANUP_CM_DIAGNOSTICS_EVENT;

    @Override
    public String event() {
        return name();
    }
}
