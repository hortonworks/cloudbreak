package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum RollingVerticalScaleState implements FlowState {

    INIT_STATE,
    ROLLING_VERTICALSCALE_STOP_INSTANCES_STATE,
    ROLLING_VERTICALSCALE_SCALE_INSTANCES_STATE,
    ROLLING_VERTICALSCALE_START_INSTANCES_STATE,
    ROLLING_VERTICALSCALE_FINISHED_STATE,
    ROLLING_VERTICALSCALE_FAILED_STATE,
    FINAL_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
