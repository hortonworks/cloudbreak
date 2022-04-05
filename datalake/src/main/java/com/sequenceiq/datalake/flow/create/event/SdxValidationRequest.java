package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxValidationRequest extends SdxEvent {

    public SdxValidationRequest(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    public SdxValidationRequest(SdxContext context) {
        super(context);
    }

    @Override
    public String selector() {
        return "SdxValidationRequest";
    }
}
