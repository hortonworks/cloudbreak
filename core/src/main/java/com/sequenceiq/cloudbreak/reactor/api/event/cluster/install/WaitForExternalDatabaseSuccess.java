package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class WaitForExternalDatabaseSuccess extends StackEvent {
    @JsonCreator
    public WaitForExternalDatabaseSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
