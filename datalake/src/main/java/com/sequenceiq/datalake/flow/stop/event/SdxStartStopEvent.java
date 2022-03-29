package com.sequenceiq.datalake.flow.stop.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.stop.SdxStopEvent;
import reactor.rx.Promise;

public class SdxStartStopEvent extends SdxEvent {

    private boolean stopDataHubs;

    public SdxStartStopEvent(String selector, Long sdxId, String userId) {
        super(selector, sdxId, userId);
        stopDataHubs = true;
    }

    public SdxStartStopEvent(String selector, Long sdxId, String userId, Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, accepted);
        stopDataHubs = true;
    }

    public SdxStartStopEvent(String selector, Long sdxId, String userId, boolean stopDataHubs, Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, accepted);
        this.stopDataHubs = stopDataHubs;
    }

    public SdxStartStopEvent(String selector, Long sdxId, String userId, boolean stopDataHubs) {
        super(selector, sdxId, userId);
        this.stopDataHubs = stopDataHubs;
    }

    public boolean stopDataHubs() {
        return stopDataHubs;
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
