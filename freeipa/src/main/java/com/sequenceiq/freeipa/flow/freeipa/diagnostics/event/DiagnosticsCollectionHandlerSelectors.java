package com.sequenceiq.freeipa.flow.freeipa.diagnostics.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum DiagnosticsCollectionHandlerSelectors implements FlowEvent {
    INIT_DIAGNOSTICS_EVENT,
    COLLECT_DIAGNOSTICS_EVENT,
    UPLOAD_DIAGNOSTICS_EVENT,
    CLEANUP_DIAGNOSTICS_EVENT;

    @Override
    public String event() {
        return name();
    }
}
