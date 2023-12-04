package com.sequenceiq.cloudbreak.structuredevent.service;

import com.sequenceiq.flow.core.FlowEvent;

public enum TestEvent implements FlowEvent {

    FAIL_HANDLED_EVENT;

    @Override
    public String event() {
        return null;
    }
}
