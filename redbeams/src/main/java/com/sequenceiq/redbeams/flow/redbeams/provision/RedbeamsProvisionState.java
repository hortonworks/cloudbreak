package com.sequenceiq.redbeams.flow.redbeams.provision;

import com.sequenceiq.flow.core.FlowState;

public enum RedbeamsProvisionState implements FlowState {
    INIT_STATE,
    REDBEAMS_PROVISION_FAILED_STATE,
    ALLOCATE_DATABASE_SERVER_STATE,
    REGISTER_DATABASE_SERVER_STATE,
    REDBEAMS_PROVISION_FINISHED_STATE,
    FINAL_STATE
}
