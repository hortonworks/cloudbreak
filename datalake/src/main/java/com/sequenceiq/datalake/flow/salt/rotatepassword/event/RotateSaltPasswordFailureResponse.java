package com.sequenceiq.datalake.flow.salt.rotatepassword.event;

import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class RotateSaltPasswordFailureResponse extends SdxFailedEvent {
    public RotateSaltPasswordFailureResponse(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId, exception);
    }
}
