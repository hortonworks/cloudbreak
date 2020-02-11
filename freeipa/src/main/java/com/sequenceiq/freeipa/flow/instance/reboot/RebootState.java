package com.sequenceiq.freeipa.flow.instance.reboot;

import com.sequenceiq.flow.core.FlowState;

public enum RebootState implements FlowState {
    INIT_STATE,
    REBOOT_FAILED_STATE,
    REBOOT_STATE,
    REBOOT_FINISHED_STATE,
    FINAL_STATE;

    RebootState() {
    }
}
