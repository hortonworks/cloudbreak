package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartBindUserCreationEvent extends StackEvent {
    @JsonCreator
    public StartBindUserCreationEvent(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
