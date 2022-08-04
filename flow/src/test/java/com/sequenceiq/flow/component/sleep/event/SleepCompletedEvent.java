package com.sequenceiq.flow.component.sleep.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.Selectable;

public class SleepCompletedEvent implements Selectable {

    private final Long resourceId;

    @JsonCreator
    public SleepCompletedEvent(@JsonProperty("resourceId") Long resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public String selector() {
        return SleepEvent.SLEEP_COMPLETED_EVENT.selector();
    }

    @Override
    public Long getResourceId() {
        return resourceId;
    }
}