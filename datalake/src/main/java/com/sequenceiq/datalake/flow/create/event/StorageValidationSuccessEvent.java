package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class StorageValidationSuccessEvent extends SdxEvent {

    public StorageValidationSuccessEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "StorageValidationSuccessEvent";
    }
}
