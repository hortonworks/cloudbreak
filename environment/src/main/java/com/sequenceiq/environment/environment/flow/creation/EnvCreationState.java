package com.sequenceiq.environment.environment.flow.creation;

import com.sequenceiq.flow.core.FlowState;

public enum EnvCreationState implements FlowState {
    INIT_STATE,
    ENVIRONMENT_CREATION_VALIDATION_STATE,
    NETWORK_CREATION_STARTED_STATE,
    FREEIPA_CREATION_STARTED_STATE,
    ENV_CREATION_FINISHED_STATE,
    ENV_CREATION_FAILED_STATE,
    FINAL_STATE
}
