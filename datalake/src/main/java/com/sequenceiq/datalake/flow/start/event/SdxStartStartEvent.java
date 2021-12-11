package com.sequenceiq.datalake.flow.start.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.datalake.flow.SdxEvent;

import reactor.rx.Promise;

public class SdxStartStartEvent extends SdxEvent {

    public SdxStartStartEvent(String selector, Long sdxId, String userId) {
        super(selector, sdxId, userId);
    }

    public SdxStartStartEvent(String selector, Long sdxId, String userId, Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, accepted);
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxStartStartEvent.class, other);
    }
}
