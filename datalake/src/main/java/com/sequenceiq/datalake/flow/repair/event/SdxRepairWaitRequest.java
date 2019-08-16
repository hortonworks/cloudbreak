package com.sequenceiq.datalake.flow.repair.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxRepairWaitRequest extends SdxEvent {

    public SdxRepairWaitRequest(Long sdxId, String userId, String requestId, String sdxCrn) {
        super(sdxId, userId, requestId, sdxCrn);
    }

    public static SdxRepairWaitRequest from(SdxContext context) {
        return new SdxRepairWaitRequest(context.getSdxId(), context.getUserId(), context.getRequestId(), context.getSdxCrn());
    }

    @Override
    public String selector() {
        return "SdxRepairWaitRequest";
    }

}

