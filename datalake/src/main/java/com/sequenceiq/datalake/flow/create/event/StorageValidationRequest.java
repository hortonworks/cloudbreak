package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class StorageValidationRequest extends SdxEvent {

    public StorageValidationRequest(SdxContext context) {
        super(context);
    }

    @Override
    public String selector() {
        return "StorageValidationRequest";
    }
}
