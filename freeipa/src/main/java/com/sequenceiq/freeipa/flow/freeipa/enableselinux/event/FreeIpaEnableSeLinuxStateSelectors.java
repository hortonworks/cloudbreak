package com.sequenceiq.freeipa.flow.freeipa.enableselinux.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum FreeIpaEnableSeLinuxStateSelectors implements FlowEvent {

    SET_SELINUX_TO_ENFORCING_EVENT,
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
