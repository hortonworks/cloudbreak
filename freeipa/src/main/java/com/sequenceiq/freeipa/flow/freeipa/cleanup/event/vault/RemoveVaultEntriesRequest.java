package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.vault;

import java.util.Set;

import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveVaultEntriesRequest extends AbstractCleanupEvent {
    public RemoveVaultEntriesRequest(CleanupEvent cleanupEvent, Stack stack, FreeIpa freeIpa) {
        super(cleanupEvent);
    }

    public RemoveVaultEntriesRequest(String selector, CleanupEvent cleanupEvent, Stack stack, FreeIpa freeIpa, Set<String> hosts) {
        super(selector, cleanupEvent);
    }
}
