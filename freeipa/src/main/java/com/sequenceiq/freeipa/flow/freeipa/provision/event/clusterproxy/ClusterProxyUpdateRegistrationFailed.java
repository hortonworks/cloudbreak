package com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class ClusterProxyUpdateRegistrationFailed extends StackFailureEvent {
    @JsonCreator
    public ClusterProxyUpdateRegistrationFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
