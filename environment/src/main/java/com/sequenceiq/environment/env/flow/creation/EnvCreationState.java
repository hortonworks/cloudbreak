package com.sequenceiq.environment.env.flow.creation;

import com.sequenceiq.flow.core.FlowState;

public enum EnvCreationState implements FlowState {
    INIT_STATE,
    NETWORK_CREATION_STARTED_STATE,
    RDBMS_CREATION_STARTED_STATE,
    FREEIPA_CREATION_STARTED_STATE,
    ENV_CREATION_FINISHED_STATE,
    ENV_CREATION_FAILED_STATE,
    FINAL_STATE
}
