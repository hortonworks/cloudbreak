package com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ModifyProxyConfigFailureResponse extends StackFailureEvent {
    @JsonCreator
    public ModifyProxyConfigFailureResponse(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
