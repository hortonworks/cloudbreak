package com.sequenceiq.freeipa.flow.freeipa.enableselinux.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum FreeIpaModifySeLinuxStateSelectors implements FlowEvent {

    MODIFY_SELINUX_START_EVENT,
    MODIFY_SELINUX_FREEIPA_EVENT,
    FINISH_MODIFY_SELINUX_FREEIPA_EVENT,
    FINALIZE_MODIFY_SELINUX_FREEIPA_EVENT,
    HANDLED_FAILED_MODIFY_SELINUX_FREEIPA_EVENT,
    FAILED_MODIFY_SELINUX_FREEIPA_EVENT;

    @Override
    public String event() {
        return name();
    }
}