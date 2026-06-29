package com.sequenceiq.freeipa.flow.freeipa.migration;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum MultiAzMigrationFinalizeState implements FlowState {

    INIT_STATE,
    MULTI_AZ_MIGRATION_FINALIZE_STATE,
    MULTI_AZ_MIGRATION_FINALIZE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
