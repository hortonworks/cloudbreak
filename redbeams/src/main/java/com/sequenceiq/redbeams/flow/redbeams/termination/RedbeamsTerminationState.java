package com.sequenceiq.redbeams.flow.redbeams.termination;

import com.sequenceiq.flow.core.FlowState;

public enum RedbeamsTerminationState implements FlowState {
    INIT_STATE,
    REDBEAMS_TERMINATION_FAILED_STATE,
    TERMINATE_DATABASE_SERVER_STATE,
    DEREGISTER_DATABASE_SERVER_STATE,
    REDBEAMS_TERMINATION_FINISHED_STATE,
    FINAL_STATE
}
