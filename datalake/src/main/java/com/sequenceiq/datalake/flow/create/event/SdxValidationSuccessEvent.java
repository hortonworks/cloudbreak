package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxValidationSuccessEvent extends SdxEvent {

    public SdxValidationSuccessEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "SdxValidationSuccessEvent";
    }
}
