package com.sequenceiq.cloudbreak.reactor.api.event.kerberos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class KeytabConfigurationFailed extends StackFailureEvent {
    @JsonCreator
    public KeytabConfigurationFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception ex) {
        super(stackId, ex);
    }
}
