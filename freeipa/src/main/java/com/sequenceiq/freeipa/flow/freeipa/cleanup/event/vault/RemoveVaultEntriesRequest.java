package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.vault;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveVaultEntriesRequest extends AbstractCleanupEvent {

    protected RemoveVaultEntriesRequest(Long stackId) {
        super(stackId);
    }

    public RemoveVaultEntriesRequest(CleanupEvent cleanupEvent) {
        super(cleanupEvent);
    }

    @JsonCreator
    public RemoveVaultEntriesRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("cleanupEvent") CleanupEvent cleanupEvent) {
        super(selector, cleanupEvent);
    }
}
