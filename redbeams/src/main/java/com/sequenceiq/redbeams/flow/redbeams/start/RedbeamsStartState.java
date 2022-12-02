package com.sequenceiq.redbeams.flow.redbeams.start;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.redbeams.flow.redbeams.common.FillInMemoryStateStoreRestartAction;

public enum RedbeamsStartState implements FlowState {
    INIT_STATE,
    REDBEAMS_START_FAILED_STATE,
    CERT_ROTATE_STATE,
    START_DATABASE_SERVER_STATE,
    REDBEAMS_START_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }

}
