package com.sequenceiq.environment.environment.flow.delete;

import com.sequenceiq.flow.core.FlowState;

public enum EnvDeleteState implements FlowState {

    INIT_STATE,
    NETWORK_DELETE_STARTED_STATE,
    RDBMS_DELETE_STARTED_STATE,
    FREEIPA_DELETE_STARTED_STATE,
    ENV_DELETE_FINISHED_STATE,
    ENV_DELETE_FAILED_STATE,
    FINAL_STATE

}
