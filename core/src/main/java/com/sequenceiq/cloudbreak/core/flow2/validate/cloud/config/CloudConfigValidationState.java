package com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config;

import com.sequenceiq.flow.core.FlowState;

public enum CloudConfigValidationState implements FlowState {

    INIT_STATE,
    VALIDATE_CLOUD_CONFIG_STATE,
    VALIDATE_CLOUD_CONFIG_FAILED_STATE,
    VALIDATE_CLOUD_CONFIG_FINISHED_STATE,
    FINAL_STATE
}
