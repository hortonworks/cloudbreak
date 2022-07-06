package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.host;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveHostsRequest extends AbstractCleanupEvent {

    protected RemoveHostsRequest(Long stackId) {
        super(stackId);
    }

    public RemoveHostsRequest(CleanupEvent cleanupEvent) {
        super(cleanupEvent);
    }

    @JsonCreator
    public RemoveHostsRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("cleanupEvent") CleanupEvent cleanupEvent) {
        super(selector, cleanupEvent);
    }
}
