package com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum CoreEnableSeLinuxStateSelectors implements FlowEvent {

    ENABLE_SELINUX_CORE_VALIDATION_EVENT,
    ENABLE_SELINUX_CORE_EVENT,
    FINISH_ENABLE_SELINUX_CORE_EVENT,
    FINALIZE_ENABLE_SELINUX_CORE_EVENT,
    HANDLED_FAILED_ENABLE_SELINUX_CORE_EVENT,
    FAILED_ENABLE_SELINUX_CORE_EVENT;

    @Override
    public String event() {
        return name();
    }
}
