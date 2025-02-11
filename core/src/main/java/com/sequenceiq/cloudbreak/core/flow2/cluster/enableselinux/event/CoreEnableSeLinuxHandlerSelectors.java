package com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum CoreEnableSeLinuxHandlerSelectors implements FlowEvent {

    ENABLE_SELINUX_CORE_VALIDATION_HANDLER,
    ENABLE_SELINUX_CORE_HANDLER;

    @Override
    public String event() {
        return name();
    }
}
