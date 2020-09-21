package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config;

import com.sequenceiq.flow.core.FlowState;

public enum ExternalDatabaseStopState implements FlowState {
    INIT_STATE,
    EXTERNAL_DATABASE_STOPPING_STATE,
    EXTERNAL_DATABASE_STOP_FAILED_STATE,
    EXTERNAL_DATABASE_STOP_FINISHED_STATE,
    FINAL_STATE
}
