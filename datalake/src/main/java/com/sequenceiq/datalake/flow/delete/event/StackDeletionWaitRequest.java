package com.sequenceiq.datalake.flow.delete.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class StackDeletionWaitRequest extends SdxEvent {

    public StackDeletionWaitRequest(Long sdxId, String userId, String requestId, String sdxCrn) {
        super(sdxId, userId, requestId, sdxCrn);
    }

    public static StackDeletionWaitRequest from(SdxContext context) {
        return new StackDeletionWaitRequest(context.getSdxId(), context.getUserId(), context.getRequestId(), context.getSdxCrn());
    }

    @Override
    public String selector() {
        return "StackDeletionWaitRequest";
    }
}
