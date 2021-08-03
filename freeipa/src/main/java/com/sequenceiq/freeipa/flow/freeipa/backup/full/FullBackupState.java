package com.sequenceiq.freeipa.flow.freeipa.backup.full;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum FullBackupState implements FlowState {
    INIT_SATE,
    BACKUP_STATE,
    BACKUP_FINISHED_STATE,
    BACKUP_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
