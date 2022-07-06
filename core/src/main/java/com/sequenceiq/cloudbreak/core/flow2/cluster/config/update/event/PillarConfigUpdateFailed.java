package com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class PillarConfigUpdateFailed extends StackFailureEvent {

    @JsonCreator
    public PillarConfigUpdateFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception ex) {
        super(stackId, ex);
    }
}
