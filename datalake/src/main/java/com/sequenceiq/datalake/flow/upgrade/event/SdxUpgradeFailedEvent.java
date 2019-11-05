package com.sequenceiq.datalake.flow.upgrade.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxUpgradeFailedEvent extends SdxEvent {

    private final Exception exception;

    public SdxUpgradeFailedEvent(Long sdxId, String userId, String requestId, Exception exception) {
        super(sdxId, userId, requestId);
        this.exception = exception;
    }

    public static SdxUpgradeFailedEvent from(SdxEvent event, Exception exception) {
        return new SdxUpgradeFailedEvent(event.getResourceId(), event.getUserId(), event.getRequestId(), exception);
    }

    @Override
    public String selector() {
        return "SdxUpgradeFailedEvent";
    }

    public Exception getException() {
        return exception;
    }
}
