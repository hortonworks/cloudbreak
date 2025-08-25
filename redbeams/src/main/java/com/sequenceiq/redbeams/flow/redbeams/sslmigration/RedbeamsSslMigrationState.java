package com.sequenceiq.redbeams.flow.redbeams.sslmigration;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.redbeams.flow.redbeams.common.FillInMemoryStateStoreRestartAction;

public enum RedbeamsSslMigrationState implements FlowState {
    INIT_STATE,
    REDBEAMS_SSL_MIGRATION_FAILED_STATE,
    REDBEAMS_SSL_MIGRATION_STATE,
    REDBEAMS_SSL_MIGRATION_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }

}
