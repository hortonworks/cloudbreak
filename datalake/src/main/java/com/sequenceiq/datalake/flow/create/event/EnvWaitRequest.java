package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class EnvWaitRequest extends SdxEvent {

    public EnvWaitRequest(Long sdxId, String userId, String requestId, String sdxCrn) {
        super(sdxId, userId, requestId, sdxCrn);
    }

    public static EnvWaitRequest from(SdxContext context) {
        return new EnvWaitRequest(context.getSdxId(), context.getUserId(), context.getRequestId(), context.getSdxCrn());
    }

    @Override
    public String selector() {
        return "EnvWaitRequest";
    }
}
