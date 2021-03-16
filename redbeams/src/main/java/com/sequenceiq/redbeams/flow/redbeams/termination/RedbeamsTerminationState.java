package com.sequenceiq.redbeams.flow.redbeams.termination;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.redbeams.flow.redbeams.common.FillInMemoryStateStoreRestartAction;

public enum RedbeamsTerminationState implements FlowState {
    INIT_STATE,
    REDBEAMS_TERMINATION_FAILED_STATE,
    TERMINATE_DATABASE_SERVER_STATE,
    DEREGISTER_DATABASE_SERVER_STATE,
    REDBEAMS_TERMINATION_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }

}
