package com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class OrchestratorConfigFailed extends StackFailureEvent {
    @JsonCreator
    public OrchestratorConfigFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
