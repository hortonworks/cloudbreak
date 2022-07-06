package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.users;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveUsersRequest extends AbstractCleanupEvent {

    protected RemoveUsersRequest(Long stackId) {
        super(stackId);
    }

    public RemoveUsersRequest(CleanupEvent cleanupEvent) {
        super(cleanupEvent);
    }

    @JsonCreator
    public RemoveUsersRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("cleanupEvent") CleanupEvent cleanupEvent) {
        super(selector, cleanupEvent);
    }
}
