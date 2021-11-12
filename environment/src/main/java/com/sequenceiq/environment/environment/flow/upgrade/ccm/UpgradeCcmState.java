package com.sequenceiq.environment.environment.flow.upgrade.ccm;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum UpgradeCcmState implements FlowState {

    INIT_STATE,
    UPGRADE_CCM_VALIDATION_STATE,
    UPGRADE_CCM_FREEIPA_STATE,
    UPGRADE_CCM_DATALAKE_STATE,
    UPGRADE_CCM_DATAHUB_STATE,
    UPGRADE_CCM_FINISHED_STATE,
    UPGRADE_CCM_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
