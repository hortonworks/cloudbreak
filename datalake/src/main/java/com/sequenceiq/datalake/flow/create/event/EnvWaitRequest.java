package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class EnvWaitRequest extends SdxEvent {

    public EnvWaitRequest(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    public static EnvWaitRequest from(SdxContext context) {
        return new EnvWaitRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String selector() {
        return "EnvWaitRequest";
    }
}
