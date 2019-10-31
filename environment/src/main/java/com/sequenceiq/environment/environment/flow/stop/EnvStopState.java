package com.sequenceiq.environment.environment.flow.stop;

import com.sequenceiq.flow.core.FlowState;

public enum EnvStopState implements FlowState {

    INIT_STATE,
    STOP_DATAHUB_STATE,
    STOP_DATALAKE_STATE,
    STOP_FREEIPA_STATE,
    ENV_STOP_FINISHED_STATE,
    ENV_STOP_FAILED_STATE,
    FINAL_STATE
}
