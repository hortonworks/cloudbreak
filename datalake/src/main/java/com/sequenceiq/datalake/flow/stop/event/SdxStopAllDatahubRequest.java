package com.sequenceiq.datalake.flow.stop.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxStopAllDatahubRequest extends SdxEvent {

    public SdxStopAllDatahubRequest(Long sdxId, String userId, String requestId) {
        super(sdxId, userId, requestId);
    }

    public static SdxStopAllDatahubRequest from(SdxContext context) {
        return new SdxStopAllDatahubRequest(context.getSdxId(), context.getUserId(), context.getRequestId());
    }

    @Override
    public String selector() {
        return "SdxStopAllDatahubRequest";
    }

}
