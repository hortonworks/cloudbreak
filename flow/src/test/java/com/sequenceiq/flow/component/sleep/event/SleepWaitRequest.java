package com.sequenceiq.flow.component.sleep.event;

import java.time.Duration;
import java.time.LocalDateTime;

import com.sequenceiq.flow.event.EventSelectorUtil;

public class SleepWaitRequest extends SleepStartEvent {

    public SleepWaitRequest(Long resourceId, Duration sleepDuration, LocalDateTime failUntil) {
        super(resourceId, sleepDuration, failUntil);
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(getClass());
    }
}