package com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum SecurityGroupValidationState implements FlowState {
    INIT_STATE,
    SECURITY_GROUP_VALIDATION_FAILED_STATE,
    SECURITY_GROUP_VALIDATION_STATE,
    SECURITY_GROUP_VALIDATION_RESULT_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
