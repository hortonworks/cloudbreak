package com.sequenceiq.datalake.flow.datalake.recovery.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeRecoveryCouldNotStartEvent extends SdxEvent {

    private final Exception exception;

    public DatalakeRecoveryCouldNotStartEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static DatalakeRecoveryCouldNotStartEvent from(SdxEvent event, Exception exception) {
        return new DatalakeRecoveryCouldNotStartEvent(event.getResourceId(), event.getUserId(), exception);
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "DatalakeRecoveryCouldNotStartEvent{" +
                "exception=" + exception +
                "} " + super.toString();
    }
}
