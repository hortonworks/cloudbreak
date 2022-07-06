package com.sequenceiq.freeipa.flow.freeipa.downscale.event.removeserver;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveServersRequest extends AbstractCleanupEvent {

    protected RemoveServersRequest(Long stackId) {
        super(stackId);
    }

    @JsonCreator
    public RemoveServersRequest(
            @JsonProperty("cleanupEvent") CleanupEvent cleanupEvent) {
        super(cleanupEvent);
    }
}
