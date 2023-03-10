package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ConfigurePolicyFailed extends StackFailureEvent {
    @JsonCreator
    public ConfigurePolicyFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception ex) {
        super(stackId, ex);
    }
}
