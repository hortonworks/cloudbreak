package com.sequenceiq.datalake.flow.modifyselinux.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum DatalakeModifySeLinuxStateSelectors implements FlowEvent {

    MODIFY_SELINUX_DATALAKE_EVENT,
    FINISH_MODIFY_SELINUX_DATALAKE_EVENT,
    FINALIZE_MODIFY_SELINUX_DATALAKE_EVENT,
    HANDLED_FAILED_MODIFY_SELINUX_DATALAKE_EVENT,
    FAILED_MODIFY_SELINUX_DATALAKE_EVENT;

    @Override
    public String event() {
        return name();
    }
}
