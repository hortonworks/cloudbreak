package com.sequenceiq.cloudbreak.reactor.api.event.kerberos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class KeytabConfigurationSuccess extends StackEvent {
    public KeytabConfigurationSuccess(Long stackId) {
        super(stackId);
    }

    @JsonCreator
    public KeytabConfigurationSuccess(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId) {
        super(selector, stackId);
    }
}
