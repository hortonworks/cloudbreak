package com.sequenceiq.environment.environment.flow.hybrid.repair;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum EnvironmentCrossRealmTrustRepairState implements FlowState {

    INIT_STATE,
    TRUST_REPAIR_VALIDATION_STATE,
    TRUST_REPAIR_STATE,
    TRUST_REPAIR_FINISHED_STATE,
    TRUST_REPAIR_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
