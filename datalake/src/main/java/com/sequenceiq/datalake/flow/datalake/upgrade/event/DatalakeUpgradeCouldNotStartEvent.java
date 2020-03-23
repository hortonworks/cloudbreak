package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradeCouldNotStartEvent extends SdxEvent {

    private final Exception exception;

    public DatalakeUpgradeCouldNotStartEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static DatalakeUpgradeCouldNotStartEvent from(SdxEvent event, Exception exception) {
        return new DatalakeUpgradeCouldNotStartEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "DatalakeUpgradeCouldNotStartEvent";
    }

    public Exception getException() {
        return exception;
    }
}
