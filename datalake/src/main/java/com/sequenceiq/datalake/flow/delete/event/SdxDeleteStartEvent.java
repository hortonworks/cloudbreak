package com.sequenceiq.datalake.flow.delete.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxDeleteStartEvent extends SdxEvent {

    private final boolean forced;

    @JsonCreator
    public SdxDeleteStartEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("forced") boolean forced) {
        super(selector, sdxId, userId);
        this.forced = forced;
    }

    public boolean isForced() {
        return forced;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxDeleteStartEvent.class, other,
                event -> forced == event.forced);
    }
}
