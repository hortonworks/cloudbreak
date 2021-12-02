package com.sequenceiq.datalake.flow.stop.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.stop.SdxStopEvent;
import reactor.rx.Promise;

public class SdxStartStopEvent extends SdxEvent {

    public SdxStartStopEvent(String selector, Long sdxId, String userId) {
        super(selector, sdxId, userId);
    }

    public SdxStartStopEvent(String selector, Long sdxId, String userId, Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, accepted);
    }

    @Override
    public String selector() {
        return SdxStopEvent.SDX_STOP_EVENT.event();
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxStartStopEvent.class, other);
    }
}
