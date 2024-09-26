package com.sequenceiq.datalake.flow.detach.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxStartDetachRecoveryEvent extends SdxEvent {
    @JsonCreator
    public SdxStartDetachRecoveryEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long detachedSdxId,
            @JsonProperty("userId") String userId) {
        super(selector, detachedSdxId, userId);
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxStartDetachRecoveryEvent.class, other,
                event -> Objects.equals(event.getResourceId(), other.getResourceId()));
    }

    @Override
    public String toString() {
        return selector() + '{' + "detachedSdxId: '" + getResourceId() + "'}";
    }
}
