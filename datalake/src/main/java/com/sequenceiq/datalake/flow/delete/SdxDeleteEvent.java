package com.sequenceiq.datalake.flow.delete;

import com.sequenceiq.flow.core.FlowEvent;

public enum SdxDeleteEvent implements FlowEvent {

    SDX_DELETE_EVENT("SDX_DELETE_EVENT"),
    SDX_STACK_DELETION_IN_PROGRESS_EVENT("SDX_STACK_DELETION_IN_PROGRESS_EVENT"),
    SDX_STACK_DELETION_SUCCESS_EVENT("StackDeletionSuccessEvent"),
    SDX_STACK_DELETION_FAILED_EVENT("SdxDeletionFailedEvent"),
    RDS_WAIT_SUCCESS_EVENT("RdsDeletionSuccessEvent"),
    SDX_DELETE_FAILED_EVENT("SDX_DELETE_FAILED_EVENT"),
    SDX_DELETE_FAILED_HANDLED_EVENT("SDX_DELETE_FAILED_HANDLED_EVENT"),
    SDX_DELETE_FINALIZED_EVENT("SDX_DELETE_FINALIZED_EVENT");

    private final String event;

    SdxDeleteEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
