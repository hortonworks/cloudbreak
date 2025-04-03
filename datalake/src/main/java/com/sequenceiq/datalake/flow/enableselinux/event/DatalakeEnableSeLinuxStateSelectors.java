package com.sequenceiq.datalake.flow.enableselinux.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum DatalakeEnableSeLinuxStateSelectors implements FlowEvent {

    ENABLE_SELINUX_DATALAKE_EVENT,
    FINISH_ENABLE_SELINUX_DATALAKE_EVENT,
    FINALIZE_ENABLE_SELINUX_DATALAKE_EVENT,
    HANDLED_FAILED_ENABLE_SELINUX_DATALAKE_EVENT,
    FAILED_ENABLE_SELINUX_DATALAKE_EVENT;

    @Override
    public String event() {
        return name();
    }
}
