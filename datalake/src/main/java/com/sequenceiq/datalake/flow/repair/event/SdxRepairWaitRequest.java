package com.sequenceiq.datalake.flow.repair.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxRepairWaitRequest extends SdxEvent {

    public SdxRepairWaitRequest(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    public static SdxRepairWaitRequest from(SdxContext context) {
        return new SdxRepairWaitRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String selector() {
        return "SdxRepairWaitRequest";
    }

}

