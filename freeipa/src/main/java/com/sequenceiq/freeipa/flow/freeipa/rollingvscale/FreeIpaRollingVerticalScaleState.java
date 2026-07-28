package com.sequenceiq.freeipa.flow.freeipa.rollingvscale;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum FreeIpaRollingVerticalScaleState implements FlowState {

    INIT_STATE,
    ROLLING_VERTICAL_SCALE_DESCRIBE_STATE,
    ROLLING_VERTICAL_SCALE_STOP_HEALTH_AGENT_STATE,
    ROLLING_VERTICAL_SCALE_STOP_SERVICES_STATE,
    ROLLING_VERTICAL_SCALE_STOP_VM_STATE,
    ROLLING_VERTICAL_SCALE_RESIZE_STATE,
    ROLLING_VERTICAL_SCALE_START_VM_STATE,
    ROLLING_VERTICAL_SCALE_HEALTH_CHECK_STATE,
    ROLLING_VERTICAL_SCALE_FINISHED_STATE,
    ROLLING_VERTICAL_SCALE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
