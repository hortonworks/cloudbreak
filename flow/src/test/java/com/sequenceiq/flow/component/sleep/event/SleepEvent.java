package com.sequenceiq.flow.component.sleep.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum SleepEvent implements FlowEvent {
    SLEEP_STARTED_EVENT,
    SLEEP_FAILED_EVENT,
    SLEEP_COMPLETED_EVENT,
    SLEEP_FINALIZED_EVENT,
    SLEEP_FAIL_HANDLED_EVENT;

    @Override
    public String event() {
        return name();
    }
}
