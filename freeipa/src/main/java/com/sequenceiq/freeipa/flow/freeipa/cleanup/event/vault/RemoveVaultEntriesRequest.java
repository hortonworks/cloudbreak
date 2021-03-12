package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.vault;

import java.util.Set;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveVaultEntriesRequest extends AbstractCleanupEvent {

    protected RemoveVaultEntriesRequest(Long stackId) {
        super(stackId);
    }

    public RemoveVaultEntriesRequest(CleanupEvent cleanupEvent, Stack stack) {
        super(cleanupEvent);
    }

    public RemoveVaultEntriesRequest(String selector, CleanupEvent cleanupEvent, Stack stack, Set<String> hosts) {
        super(selector, cleanupEvent);
    }
}
