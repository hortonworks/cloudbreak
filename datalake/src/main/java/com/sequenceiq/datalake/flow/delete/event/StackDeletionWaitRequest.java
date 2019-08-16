package com.sequenceiq.datalake.flow.delete.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class StackDeletionWaitRequest extends SdxEvent {

    public StackDeletionWaitRequest(Long sdxId, String userId, String requestId) {
        super(sdxId, userId, requestId);
    }

    public static StackDeletionWaitRequest from(SdxContext context) {
        return new StackDeletionWaitRequest(context.getSdxId(), context.getUserId(), context.getRequestId());
    }

    @Override
    public String selector() {
        return "StackDeletionWaitRequest";
    }
}
