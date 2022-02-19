package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum CcmUpgradeState implements FlowState {
    INIT_STATE,
    CCM_UPGRADE_FAILED_STATE,
    CCM_UPGRADE_PREPARATION_FAILED,

    CCM_UPGRADE_PREPARATION_STATE,
    CCM_UPGRADE_RE_REGISTER_TO_CP,
    CCM_UPGRADE_REMOVE_AUTOSSH,
    CCM_UPGRADE_UNREGISTER_HOSTS,
    CCM_UPGRADE_FINISHED_STATE,

    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    CcmUpgradeState() {

    }

    CcmUpgradeState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
