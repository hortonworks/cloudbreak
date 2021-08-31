package com.sequenceiq.flow.component.sleep.event;

import com.sequenceiq.cloudbreak.common.event.Selectable;

public class SleepFailedEvent implements Selectable {

    private final Long resourceId;

    private final String reason;

    public SleepFailedEvent(Long resourceId, String reason) {
        this.resourceId = resourceId;
        this.reason = reason;
    }

    @Override
    public String selector() {
        return SleepEvent.SLEEP_FAILED_EVENT.selector();
    }

    @Override
    public Long getResourceId() {
        return resourceId;
    }

    public String getReason() {
        return reason;
    }
}