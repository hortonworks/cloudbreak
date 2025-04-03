package com.sequenceiq.flow.component.sleep.event;

import java.time.Duration;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class SleepWaitRequest extends SleepStartEvent {

    @JsonCreator
    public SleepWaitRequest(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("sleepDuration") Duration sleepDuration,
            @JsonProperty("failUntil") LocalDateTime failUntil) {
        super(resourceId, sleepDuration, failUntil, new Promise<>());
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(getClass());
    }
}
