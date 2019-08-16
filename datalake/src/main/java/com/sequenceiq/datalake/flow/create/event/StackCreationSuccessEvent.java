package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class StackCreationSuccessEvent extends SdxEvent {

    public StackCreationSuccessEvent(Long sdxId, String userId, String requestId, String sdxCrn) {
        super(sdxId, userId, requestId, sdxCrn);
    }

    @Override
    public String selector() {
        return "StackCreationSuccessEvent";
    }

}
