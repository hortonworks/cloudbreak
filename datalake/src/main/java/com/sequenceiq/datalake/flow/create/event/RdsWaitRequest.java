package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class RdsWaitRequest extends SdxEvent {

    public RdsWaitRequest(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    public RdsWaitRequest(SdxContext context) {
        super(context);
    }

    @Override
    public String selector() {
        return "RdsWaitRequest";
    }
}
