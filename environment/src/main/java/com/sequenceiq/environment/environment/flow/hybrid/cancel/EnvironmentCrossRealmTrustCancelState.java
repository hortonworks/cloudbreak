package com.sequenceiq.environment.environment.flow.hybrid.cancel;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum EnvironmentCrossRealmTrustCancelState implements FlowState {

    INIT_STATE,
    TRUST_CANCEL_VALIDATION_STATE,
    TRUST_CANCEL_STATE,
    TRUST_CANCEL_FINISHED_STATE,
    TRUST_CANCEL_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
