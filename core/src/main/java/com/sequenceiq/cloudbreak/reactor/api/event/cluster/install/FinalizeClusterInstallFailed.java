package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class FinalizeClusterInstallFailed extends StackFailureEvent {
    @JsonCreator
    public FinalizeClusterInstallFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception ex) {
        super(stackId, ex);
    }
}
