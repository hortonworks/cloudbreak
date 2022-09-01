package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ExecutePostServiceDeploymentRecipesFailed extends StackFailureEvent {
    @JsonCreator
    public ExecutePostServiceDeploymentRecipesFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception ex) {
        super(stackId, ex);
    }
}
