package com.sequenceiq.redbeams.flow.redbeams.start;

import com.sequenceiq.flow.core.FlowState;

public enum RedbeamsStartState implements FlowState {
    INIT_STATE,
    REDBEAMS_START_FAILED_STATE,
    START_DATABASE_SERVER_STATE,
    REDBEAMS_START_FINISHED_STATE,
    FINAL_STATE;
}
