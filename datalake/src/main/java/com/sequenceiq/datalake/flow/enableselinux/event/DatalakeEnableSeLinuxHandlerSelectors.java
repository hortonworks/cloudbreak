package com.sequenceiq.datalake.flow.enableselinux.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum DatalakeEnableSeLinuxHandlerSelectors implements FlowEvent {

    ENABLE_SELINUX_DATALAKE_VALIDATION_HANDLER,
    ENABLE_SELINUX_DATALAKE_HANDLER;

    @Override
    public String event() {
        return name();
    }
}
