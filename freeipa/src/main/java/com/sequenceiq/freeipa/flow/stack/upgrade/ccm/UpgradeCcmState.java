package com.sequenceiq.freeipa.flow.stack.upgrade.ccm;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.freeipa.flow.InitializeMDCContextRestartAction;

public enum UpgradeCcmState implements FlowState {

    INIT_STATE,

    UPGRADE_CCM_CHECK_PREREQUISITES_STATE,
    // TODO further states

    UPGRADE_CCM_FAILED_STATE,
    UPGRADE_CCM_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        // TODO restartAction
        Class<? extends RestartAction> action = FillInMemoryStateStoreRestartAction.class;
        return InitializeMDCContextRestartAction.class;
    }

}
