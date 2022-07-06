package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.roles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveRolesRequest extends AbstractCleanupEvent {

    protected RemoveRolesRequest(Long stackId) {
        super(stackId);
    }

    public RemoveRolesRequest(CleanupEvent cleanupEvent) {
        super(cleanupEvent);
    }

    @JsonCreator
    public RemoveRolesRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("cleanupEvent") CleanupEvent cleanupEvent) {
        super(selector, cleanupEvent);
    }
}
