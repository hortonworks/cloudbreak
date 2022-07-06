package com.sequenceiq.cloudbreak.reactor.api.event.kerberos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class KeytabConfigurationRequest extends StackEvent {
    @JsonCreator
    public KeytabConfigurationRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
