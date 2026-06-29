package com.sequenceiq.freeipa.flow.freeipa.migration;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum MultiAzMigrationInitState implements FlowState {

    INIT_STATE,
    MULTI_AZ_MIGRATION_INIT_STATE,
    MULTI_AZ_MIGRATION_INIT_FINISHED_STATE,
    MULTI_AZ_MIGRATION_INIT_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
