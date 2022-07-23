package com.sequenceiq.datalake.flow.stop.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.stop.SdxStopEvent;

import reactor.rx.Promise;

public class SdxStartStopEvent extends SdxEvent {

    private final boolean stopDataHubs;

    public SdxStartStopEvent(String selector, Long sdxId, String userId) {
        super(selector, sdxId, userId);
        stopDataHubs = true;
    }

    public SdxStartStopEvent(String selector, Long sdxId, String userId, Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, accepted);
        stopDataHubs = true;
    }

    @JsonCreator
    public SdxStartStopEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("stopDataHubs") boolean stopDataHubs,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, accepted);
        this.stopDataHubs = stopDataHubs;
    }

    public SdxStartStopEvent(String selector, Long sdxId, String userId, boolean stopDataHubs) {
        super(selector, sdxId, userId);
        this.stopDataHubs = stopDataHubs;
    }

    public boolean isStopDataHubs() {
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
