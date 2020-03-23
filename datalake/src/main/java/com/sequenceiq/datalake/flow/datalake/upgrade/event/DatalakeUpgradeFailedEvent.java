package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradeFailedEvent extends SdxEvent {

    private final Exception exception;

    public DatalakeUpgradeFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static DatalakeUpgradeFailedEvent from(SdxEvent event, Exception exception) {
        return new DatalakeUpgradeFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "DatalakeUpgradeFailedEvent";
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DatalakeUpgradeFailedEvent{");
        sb.append("exception=").append(exception);
        sb.append('}');
        return sb.toString();
    }
}
