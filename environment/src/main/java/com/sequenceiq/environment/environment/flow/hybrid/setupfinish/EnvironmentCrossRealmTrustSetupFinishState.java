package com.sequenceiq.environment.environment.flow.hybrid.setupfinish;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum EnvironmentCrossRealmTrustSetupFinishState implements FlowState {

    INIT_STATE,
    SETUP_FINISH_TRUST_VALIDATION_STATE,
    SETUP_FINISH_TRUST_STATE,
    SETUP_FINISH_TRUST_FINISHED_STATE,
    SETUP_FINISH_TRUST_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
