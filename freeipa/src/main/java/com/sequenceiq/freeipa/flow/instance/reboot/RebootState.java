package com.sequenceiq.freeipa.flow.instance.reboot;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum RebootState implements FlowState {
    INIT_STATE,
    REBOOT_FAILED_STATE,
    REBOOT_STATE,
    REBOOT_WAIT_UNTIL_AVAILABLE_STATE,
    REBOOT_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
