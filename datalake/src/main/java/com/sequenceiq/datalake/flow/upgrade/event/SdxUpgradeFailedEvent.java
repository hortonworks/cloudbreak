package com.sequenceiq.datalake.flow.upgrade.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxUpgradeFailedEvent extends SdxEvent {

    private final Exception exception;

    public SdxUpgradeFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static SdxUpgradeFailedEvent from(SdxEvent event, Exception exception) {
        return new SdxUpgradeFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "SdxUpgradeFailedEvent";
    }

    public Exception getException() {
        return exception;
    }
}
