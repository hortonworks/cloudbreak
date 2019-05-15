package com.sequenceiq.datalake.flow.create;

import com.sequenceiq.flow.core.FlowEvent;

public enum SdxCreateEvent implements FlowEvent {

    SDX_CREATE_EVENT("SDX_CREATE_EVENT"),
    SDX_STACK_CREATION_IN_PROGRESS_EVENT("SDX_STACK_CREATION_IN_PROGRESS_EVENT"),
    SDX_STACK_CREATION_SUCCESS_EVENT("StackCreationSuccessEvent"),
    SDX_STACK_CREATION_FAILED_EVENT("StackCreationFailedEvent"),
    SDX_CREATE_FAILED_EVENT("SDX_CREATE_FAILED_EVENT"),
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
