package com.sequenceiq.datalake.flow.start.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxStartWaitRequest extends SdxEvent {

    public SdxStartWaitRequest(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    public static SdxStartWaitRequest from(SdxContext context) {
        return new SdxStartWaitRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String selector() {
        return "SdxStartWaitRequest";
    }

}

