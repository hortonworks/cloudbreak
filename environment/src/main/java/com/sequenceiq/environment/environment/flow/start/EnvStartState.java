package com.sequenceiq.environment.environment.flow.start;

import com.sequenceiq.flow.core.FlowState;

public enum EnvStartState implements FlowState {

    INIT_STATE,
    START_DATAHUB_STATE,
    START_DATALAKE_STATE,
    START_FREEIPA_STATE,
    ENV_START_FINISHED_STATE,
    ENV_START_FAILED_STATE,
    FINAL_STATE
}
