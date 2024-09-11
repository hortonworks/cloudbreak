package com.sequenceiq.freeipa.flow.stack.stop;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class StopFreeIpaServicesEvent extends StackEvent {
    @JsonCreator
    public StopFreeIpaServicesEvent(@JsonProperty("resourceId") Long resourceId) {
        super(resourceId);
    }
}
