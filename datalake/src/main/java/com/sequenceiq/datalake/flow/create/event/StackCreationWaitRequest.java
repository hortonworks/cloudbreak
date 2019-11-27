package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class StackCreationWaitRequest extends SdxEvent {

    public StackCreationWaitRequest(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    public static StackCreationWaitRequest from(SdxContext context) {
        return new StackCreationWaitRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String selector() {
        return "StackCreationWaitRequest";
    }
}
