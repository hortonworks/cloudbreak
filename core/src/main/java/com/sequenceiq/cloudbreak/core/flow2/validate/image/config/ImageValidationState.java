package com.sequenceiq.cloudbreak.core.flow2.validate.image.config;

import com.sequenceiq.cloudbreak.core.flow2.restart.InitializeMDCContextRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum ImageValidationState implements FlowState {

    INIT_STATE,
    IMAGE_VALIDATION_STATE,
    IMAGE_VALIDATION_FAILED_STATE,
    IMAGE_VALIDATION_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return InitializeMDCContextRestartAction.class;
    }
}
