package com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class ClusterProxyRegistrationFailed extends StackFailureEvent {
    @JsonCreator
    public ClusterProxyRegistrationFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
