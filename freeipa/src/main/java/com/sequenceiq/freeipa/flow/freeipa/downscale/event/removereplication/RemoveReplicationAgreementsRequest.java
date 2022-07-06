package com.sequenceiq.freeipa.flow.freeipa.downscale.event.removereplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveReplicationAgreementsRequest extends AbstractCleanupEvent {

    protected RemoveReplicationAgreementsRequest(Long stackId) {
        super(stackId);
    }

    @JsonCreator
    public RemoveReplicationAgreementsRequest(
            @JsonProperty("cleanupEvent") CleanupEvent cleanupEvent) {
        super(cleanupEvent);
    }
}
