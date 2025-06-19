package com.sequenceiq.freeipa.flow.freeipa.trust.setup.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ConfigureDnsRequest extends StackEvent {

    @JsonCreator
    public ConfigureDnsRequest(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
