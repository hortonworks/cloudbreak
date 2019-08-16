package com.sequenceiq.datalake.flow.delete.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class StackDeletionSuccessEvent extends SdxEvent {

    public StackDeletionSuccessEvent(Long sdxId, String userId, String requestId, String sdxCrn) {
        super(sdxId, userId, requestId, sdxCrn);
    }

    @Override
    public String selector() {
        return "StackDeletionSuccessEvent";
    }
}

