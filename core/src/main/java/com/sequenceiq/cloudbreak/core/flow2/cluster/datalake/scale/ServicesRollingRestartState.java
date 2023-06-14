package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum ServicesRollingRestartState implements FlowState {
    INIT_STATE,
    ROLLING_RESTART_FAILED_STATE,
    ROLLING_RESTART_STATE,
    ROLLING_RESTART_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    ServicesRollingRestartState() {

    }

    ServicesRollingRestartState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return null;
    }
}
