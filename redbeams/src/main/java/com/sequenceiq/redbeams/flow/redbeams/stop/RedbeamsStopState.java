package com.sequenceiq.redbeams.flow.redbeams.stop;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.redbeams.flow.redbeams.common.FillInMemoryStateStoreRestartAction;

public enum RedbeamsStopState implements FlowState {
    INIT_STATE,
    REDBEAMS_STOP_FAILED_STATE,
    STOP_DATABASE_SERVER_STATE,
    REDBEAMS_STOP_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }

}
