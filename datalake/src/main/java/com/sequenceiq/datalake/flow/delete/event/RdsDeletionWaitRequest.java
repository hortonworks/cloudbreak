package com.sequenceiq.datalake.flow.delete.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class RdsDeletionWaitRequest extends SdxEvent {

    private final boolean forced;

    public RdsDeletionWaitRequest(Long sdxId, String userId, boolean forced) {
        super(sdxId, userId);
        this.forced = forced;
    }

    public static RdsDeletionWaitRequest from(SdxContext context, StackDeletionSuccessEvent payload) {
        return new RdsDeletionWaitRequest(context.getSdxId(), context.getUserId(), payload.isForced());
    }

    @Override
    public String selector() {
        return "RdsDeletionWaitRequest";
    }

    public boolean isForced() {
        return forced;
    }
}
