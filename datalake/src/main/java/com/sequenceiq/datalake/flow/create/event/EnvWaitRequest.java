package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class EnvWaitRequest extends SdxEvent {

    public EnvWaitRequest(Long sdxId, String userId, String requestId) {
        super(sdxId, userId, requestId);
    }

    public static EnvWaitRequest from(SdxContext context) {
        return new EnvWaitRequest(context.getSdxId(), context.getUserId(), context.getRequestId());
    }

    @Override
    public String selector() {
        return "EnvWaitRequest";
    }
}
