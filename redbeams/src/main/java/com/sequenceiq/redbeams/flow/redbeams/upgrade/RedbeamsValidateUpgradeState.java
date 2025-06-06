package com.sequenceiq.redbeams.flow.redbeams.upgrade;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.redbeams.flow.redbeams.common.FillInMemoryStateStoreRestartAction;

public enum RedbeamsValidateUpgradeState implements FlowState {

    INIT_STATE,
    REDBEAMS_VALIDATE_UPGRADE_FAILED_STATE,
    VALIDATE_UPGRADE_DATABASE_SERVER_STATE,
    REDBEAMS_VALIDATE_UPGRADE_FINISHED_STATE,
    FINAL_STATE;

    private final Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }

}