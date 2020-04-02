package com.sequenceiq.redbeams.flow.redbeams.stop;

import com.sequenceiq.flow.core.FlowState;

public enum RedbeamsStopState implements FlowState {
    INIT_STATE,
    REDBEAMS_STOP_FAILED_STATE,
    STOP_DATABASE_SERVER_STATE,
    REDBEAMS_STOP_FINISHED_STATE,
    FINAL_STATE;
}
