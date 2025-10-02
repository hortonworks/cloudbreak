package com.sequenceiq.environment.environment.flow.hybrid.setupfinish;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum EnvironmentCrossRealmTrustSetupFinishState implements FlowState {

    INIT_STATE,
    TRUST_SETUP_FINISH_VALIDATION_STATE,
    TRUST_SETUP_FINISH_STATE,
    TRUST_SETUP_FINISH_FINISHED_STATE,
    TRUST_SETUP_FINISH_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
