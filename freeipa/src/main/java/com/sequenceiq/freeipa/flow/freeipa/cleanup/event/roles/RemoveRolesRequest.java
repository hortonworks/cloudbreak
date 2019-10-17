package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.roles;

import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveRolesRequest extends AbstractCleanupEvent {
    public RemoveRolesRequest(CleanupEvent cleanupEvent, Stack stack, FreeIpa freeIpa) {
        super(cleanupEvent);
    }

    public RemoveRolesRequest(String selector, CleanupEvent cleanupEvent, Stack stack, FreeIpa freeIpa) {
        super(selector, cleanupEvent);
    }
}
