package com.sequenceiq.freeipa.flow.freeipa.downscale.event.removehosts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveHostsFromOrchestrationRequest extends AbstractCleanupEvent {

    protected RemoveHostsFromOrchestrationRequest(Long stackId) {
        super(stackId);
    }

    @JsonCreator
    public RemoveHostsFromOrchestrationRequest(
            @JsonProperty("cleanupEvent") CleanupEvent cleanupEvent) {
        super(cleanupEvent);
    }
}
