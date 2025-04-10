package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum CoreModifySeLinuxStateSelectors implements FlowEvent {

    CORE_MODIFY_SELINUX_EVENT,
    MODIFY_SELINUX_CORE_EVENT,
    FINISH_MODIFY_SELINUX_CORE_EVENT,
    FINALIZE_MODIFY_SELINUX_CORE_EVENT,
    HANDLED_FAILED_MODIFY_SELINUX_CORE_EVENT,
    FAILED_MODIFY_SELINUX_CORE_EVENT;

    @Override
    public String event() {
        return name();
    }
}
