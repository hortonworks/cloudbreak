package com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvironmentEnableSeLinuxStateSelectors implements FlowEvent {

    ENABLE_SELINUX_FREEIPA_VALIDATION_EVENT,
    ENABLE_SELINUX_FREEIPA_EVENT,
    FINISH_ENABLE_SELINUX_FREEIPA_EVENT,
    FINALIZE_ENABLE_SELINUX_FREEIPA_EVENT,
    HANDLED_FAILED_ENABLE_SELINUX_FREEIPA_EVENT,
    FAILED_ENABLE_SELINUX_FREEIPA_EVENT;

    @Override
    public String event() {
        return name();
    }
}
