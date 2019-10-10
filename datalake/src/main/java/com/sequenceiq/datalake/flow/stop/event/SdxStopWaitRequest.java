package com.sequenceiq.datalake.flow.stop.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxStopWaitRequest extends SdxEvent {

    public SdxStopWaitRequest(Long sdxId, String userId, String requestId) {
        super(sdxId, userId, requestId);
    }

    public static SdxStopWaitRequest from(SdxContext context) {
        return new SdxStopWaitRequest(context.getSdxId(), context.getUserId(), context.getRequestId());
    }

    @Override
    public String selector() {
        return "SdxStopWaitRequest";
    }

}

