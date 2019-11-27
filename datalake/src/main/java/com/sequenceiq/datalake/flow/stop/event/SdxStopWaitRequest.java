package com.sequenceiq.datalake.flow.stop.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxStopWaitRequest extends SdxEvent {

    public SdxStopWaitRequest(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    public static SdxStopWaitRequest from(SdxContext context) {
        return new SdxStopWaitRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String selector() {
        return "SdxStopWaitRequest";
    }

}

