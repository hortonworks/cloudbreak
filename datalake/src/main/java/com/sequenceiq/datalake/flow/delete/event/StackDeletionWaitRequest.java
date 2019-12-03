package com.sequenceiq.datalake.flow.delete.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class StackDeletionWaitRequest extends SdxEvent {

    private final boolean forced;

    public StackDeletionWaitRequest(Long sdxId, String userId, boolean forced) {
        super(sdxId, userId);
        this.forced = forced;
    }

    public static StackDeletionWaitRequest from(SdxContext context, SdxDeleteStartEvent payload) {
        return new StackDeletionWaitRequest(context.getSdxId(), context.getUserId(), payload.isForced());
    }

    @Override
    public String selector() {
        return "StackDeletionWaitRequest";
    }

    public boolean isForced() {
        return forced;
    }
}
