package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class StackCreationWaitRequest extends SdxEvent {

    public StackCreationWaitRequest(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "StackCreationWaitRequest";
    }
}
