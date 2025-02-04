package com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvironmentEnableSeLinuxHandlerSelectors implements FlowEvent {

    ENABLE_SELINUX_FREEIPA_VALIDATION_HANDLER,
    ENABLE_SELINUX_FREEIPA_HANDLER;

    @Override
    public String event() {
        return name();
    }
}
