package com.sequenceiq.freeipa.flow.freeipa.downscale.event.replicationcleanup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class VerifyReplicationCleanupRequest extends AbstractCleanupEvent {

    protected VerifyReplicationCleanupRequest(Long stackId) {
        super(stackId);
    }

    @JsonCreator
    public VerifyReplicationCleanupRequest(
            @JsonProperty("cleanupEvent") CleanupEvent cleanupEvent) {
        super(cleanupEvent);
    }
}
