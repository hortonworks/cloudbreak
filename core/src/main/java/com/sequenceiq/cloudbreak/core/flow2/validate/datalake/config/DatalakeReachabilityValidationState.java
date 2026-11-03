package com.sequenceiq.cloudbreak.core.flow2.validate.datalake.config;

import com.sequenceiq.cloudbreak.core.flow2.restart.InitializeMDCContextRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DatalakeReachabilityValidationState implements FlowState {

    INIT_STATE,
    DATALAKE_REACHABILITY_VALIDATION_STATE,
    DATALAKE_REACHABILITY_VALIDATION_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return InitializeMDCContextRestartAction.class;
    }
}
