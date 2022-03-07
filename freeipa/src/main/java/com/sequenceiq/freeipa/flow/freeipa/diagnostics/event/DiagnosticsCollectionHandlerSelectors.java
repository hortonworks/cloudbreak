package com.sequenceiq.freeipa.flow.freeipa.diagnostics.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum DiagnosticsCollectionHandlerSelectors implements FlowEvent {
    SALT_VALIDATION_DIAGNOSTICS_EVENT,
    SALT_PILLAR_UPDATE_DIAGNOSTICS_EVENT,
    SALT_STATE_UPDATE_DIAGNOSTICS_EVENT,
    PREFLIGHT_CHECK_DIAGNOSTICS_EVENT,
    INIT_DIAGNOSTICS_EVENT,
    UPGRADE_DIAGNOSTICS_EVENT,
    VM_PREFLIGHT_CHECK_DIAGNOSTICS_EVENT,
    ENSURE_MACHINE_USER_EVENT,
    COLLECT_DIAGNOSTICS_EVENT,
    UPLOAD_DIAGNOSTICS_EVENT,
    CLEANUP_DIAGNOSTICS_EVENT;

    @Override
    public String event() {
        return name();
    }
}
