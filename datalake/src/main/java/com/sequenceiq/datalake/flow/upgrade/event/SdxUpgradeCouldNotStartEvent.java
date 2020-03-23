package com.sequenceiq.datalake.flow.upgrade.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxUpgradeCouldNotStartEvent extends SdxEvent {

    private Exception exception;

    public SdxUpgradeCouldNotStartEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static SdxUpgradeCouldNotStartEvent from(SdxEvent event, Exception exception) {
        return new SdxUpgradeCouldNotStartEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "SdxRepairCouldNotStartEvent";
    }

    public Exception getException() {
        return exception;
    }
}
