package com.sequenceiq.datalake.flow.delete.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class StackDeletionWaitRequest extends SdxEvent {

    public StackDeletionWaitRequest(Long sdxId, String userId, String requestId) {
        super(sdxId, userId, requestId);
    }

    @Override
    public String selector() {
        return "StackDeletionWaitRequest";
    }
}
