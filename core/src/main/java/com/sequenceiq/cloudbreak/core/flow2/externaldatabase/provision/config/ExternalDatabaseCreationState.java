package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config;

import com.sequenceiq.flow.core.FlowState;

public enum ExternalDatabaseCreationState implements FlowState {
    INIT_STATE,
    WAIT_FOR_EXTERNAL_DATABASE_STATE,
    EXTERNAL_DATABASE_CREATION_FAILED_STATE,
    EXTERNAL_DATABASE_CREATION_FINISHED_STATE,
    FINAL_STATE
}
