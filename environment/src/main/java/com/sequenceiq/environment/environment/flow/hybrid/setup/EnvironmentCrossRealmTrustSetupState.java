package com.sequenceiq.environment.environment.flow.hybrid.setup;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum EnvironmentCrossRealmTrustSetupState implements FlowState {

    INIT_STATE,
    TRUST_SETUP_VALIDATION_STATE,
    TRUST_SETUP_STATE,
    TRUST_SETUP_FINISHED_STATE,
    TRUST_SETUP_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
