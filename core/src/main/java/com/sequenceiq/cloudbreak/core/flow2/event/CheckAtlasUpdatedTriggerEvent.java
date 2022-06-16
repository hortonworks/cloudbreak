package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.atlas.CheckAtlasUpdatedStackEvent;

public class CheckAtlasUpdatedTriggerEvent extends CheckAtlasUpdatedStackEvent {
    @JsonCreator
    public CheckAtlasUpdatedTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId) {
        super(selector, stackId);
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(CheckAtlasUpdatedTriggerEvent.class, other,
                event -> Objects.equals(other.getResourceId(), event.getResourceId()));
    }
}
