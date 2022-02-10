package com.sequenceiq.datalake.flow.upgrade.ccm;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum UpgradeCcmState implements FlowState {

    INIT_STATE,
    UPGRADE_CCM_UPGRADE_STACK_STATE,
    UPGRADE_CCM_FINISHED_STATE,
    UPGRADE_CCM_FAILED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    UpgradeCcmState() {
    }

    UpgradeCcmState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }

}
