package com.sequenceiq.datalake.flow.detach.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxDetachRecoveryFailedEvent extends SdxFailedEvent {
    public SdxDetachRecoveryFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId, exception);
    }

    public static SdxDetachRecoveryFailedEvent from(SdxEvent event, Exception exception) {
        return new SdxDetachRecoveryFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }
}
