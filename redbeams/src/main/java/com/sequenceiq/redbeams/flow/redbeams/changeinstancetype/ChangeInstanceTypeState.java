package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.redbeams.flow.redbeams.common.FillInMemoryStateStoreRestartAction;

public enum ChangeInstanceTypeState implements FlowState {

    INIT_STATE,
    CHANGE_INSTANCE_TYPE_FAILED_STATE,
    CHANGE_INSTANCE_TYPE_SYNC_STATE,
    CHANGE_INSTANCE_TYPE_VALIDATION_STATE,
    CHANGE_INSTANCE_TYPE_MODIFY_STATE,
    CHANGE_INSTANCE_TYPE_WAIT_STATE,
    CHANGE_INSTANCE_TYPE_FINISHED_STATE,
    FINAL_STATE;

    private final Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
