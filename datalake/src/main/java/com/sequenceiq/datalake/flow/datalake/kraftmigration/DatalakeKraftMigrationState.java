package com.sequenceiq.datalake.flow.datalake.kraftmigration;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum DatalakeKraftMigrationState implements FlowState {

    INIT_STATE,
    DATALAKE_KRAFT_MIGRATION_START_STATE,
    DATALAKE_KRAFT_MIGRATION_IN_PROGRESS_STATE,
    DATALAKE_KRAFT_MIGRATION_FAILED_STATE,
    DATALAKE_KRAFT_MIGRATION_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
