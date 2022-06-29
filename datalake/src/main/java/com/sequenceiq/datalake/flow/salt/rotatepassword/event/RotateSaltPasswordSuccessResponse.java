package com.sequenceiq.datalake.flow.salt.rotatepassword.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class RotateSaltPasswordSuccessResponse extends SdxEvent {
    public RotateSaltPasswordSuccessResponse(Long sdxId, String userId) {
        super(sdxId, userId);
    }
}
