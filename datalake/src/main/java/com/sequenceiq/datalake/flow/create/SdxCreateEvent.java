package com.sequenceiq.datalake.flow.create;

import com.sequenceiq.flow.core.FlowEvent;

public enum SdxCreateEvent implements FlowEvent {

    STORAGE_VALIDATION_WAIT_EVENT("STORAGE_VALIDATION_WAIT_EVENT"),
    STORAGE_VALIDATION_SUCCESS_EVENT("StorageValidationSuccessEvent"),
    ENV_WAIT_SUCCESS_EVENT("EnvWaitSuccessEvent"),
    RDS_WAIT_SUCCESS_EVENT("RdsWaitSuccessEvent"),
    SDX_STACK_CREATION_IN_PROGRESS_EVENT("SDX_STACK_CREATION_IN_PROGRESS_EVENT"),
    SDX_STACK_CREATION_SUCCESS_EVENT("StackCreationSuccessEvent"),
    SDX_CREATE_FAILED_EVENT("SdxCreateFailedEvent"),
    SDX_CREATE_FAILED_HANDLED_EVENT("SDX_CREATE_FAILED_HANDLED_EVENT"),
    SDX_CREATE_FINALIZED_EVENT("SDX_CREATE_FINALIZED_EVENT");

    private final String event;

    SdxCreateEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
