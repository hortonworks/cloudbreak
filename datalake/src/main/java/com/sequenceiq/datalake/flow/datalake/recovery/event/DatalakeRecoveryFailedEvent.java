package com.sequenceiq.datalake.flow.datalake.recovery.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class DatalakeRecoveryFailedEvent extends SdxFailedEvent {

    public DatalakeRecoveryFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId, exception);
    }

    public static DatalakeRecoveryFailedEvent from(SdxEvent event, Exception exception) {
        return new DatalakeRecoveryFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String toString() {
        return "DatalakeRecoveryFailedEvent{} " + super.toString();
    }
}
