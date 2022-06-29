package com.sequenceiq.datalake.flow.salt.rotatepassword.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class RotateSaltPasswordWaitRequest extends SdxEvent {
    public RotateSaltPasswordWaitRequest(Long sdxId, String userId) {
        super(sdxId, userId);
    }
}
