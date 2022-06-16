package com.sequenceiq.datalake.flow.atlas.updated.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.datalake.flow.SdxEvent;

import reactor.rx.Promise;

public class StartCheckAtlasUpdatedEvent extends SdxEvent {
    @JsonCreator
    public StartCheckAtlasUpdatedEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonIgnoreDeserialization Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, accepted);
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(StartCheckAtlasUpdatedEvent.class, other,
                event -> Objects.equals(event.getResourceId(), other.getResourceId()));
    }
}
