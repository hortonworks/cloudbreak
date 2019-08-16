package com.sequenceiq.datalake.flow.delete.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class RdsDeletionWaitRequest extends SdxEvent {

    public RdsDeletionWaitRequest(Long sdxId, String userId, String requestId, String sdxCrn) {
        super(sdxId, userId, requestId, sdxCrn);
    }

    public static RdsDeletionWaitRequest from(SdxContext context) {
        return new RdsDeletionWaitRequest(context.getSdxId(), context.getUserId(), context.getRequestId(), context.getSdxCrn());
    }

    @Override
    public String selector() {
        return "RdsDeletionWaitRequest";
    }
}
