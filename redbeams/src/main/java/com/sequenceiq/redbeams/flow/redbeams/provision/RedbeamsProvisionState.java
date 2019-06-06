package com.sequenceiq.redbeams.flow.redbeams.provision;

import com.sequenceiq.flow.core.FlowState;

public enum RedbeamsProvisionState implements FlowState {
    INIT_STATE,
    REDBEAMS_PROVISION_FAILED_STATE,
    ALLOCATE_DATABASE_STATE,
    ALLOCATE_DATABASE_FINISHED_STATE,
    FINAL_STATE
}
