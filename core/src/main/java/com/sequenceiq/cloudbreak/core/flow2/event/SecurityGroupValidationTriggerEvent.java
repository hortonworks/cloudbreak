package com.sequenceiq.cloudbreak.core.flow2.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class SecurityGroupValidationTriggerEvent extends StackEvent {

    @JsonCreator
    public SecurityGroupValidationTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId) {
        super(selector, stackId);
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(SecurityGroupValidationTriggerEvent.class, other);
    }
}
