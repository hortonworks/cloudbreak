package com.sequenceiq.datalake.flow.create.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class EnvWaitRequest extends SdxEvent {

    public EnvWaitRequest(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "EnvWaitRequest";
    }
}
