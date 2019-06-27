package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class StackCreationSuccessEvent extends SdxEvent {

    public StackCreationSuccessEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "StackCreationSuccessEvent";
    }

}
