package com.sequenceiq.environment.environment.flow.verticalscale.freeipa;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum EnvironmentVerticalScaleState implements FlowState {

    INIT_STATE,
    VERTICAL_SCALING_FREEIPA_VALIDATION_STATE,
    VERTICAL_SCALING_FREEIPA_STATE,
    VERTICAL_SCALING_FREEIPA_FINISHED_STATE,
    VERTICAL_SCALING_FREEIPA_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
