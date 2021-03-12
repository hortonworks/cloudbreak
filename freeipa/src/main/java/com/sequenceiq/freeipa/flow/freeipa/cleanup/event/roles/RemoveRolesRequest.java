package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.roles;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveRolesRequest extends AbstractCleanupEvent {

    protected RemoveRolesRequest(Long stackId) {
        super(stackId);
    }

    public RemoveRolesRequest(CleanupEvent cleanupEvent, Stack stack) {
        super(cleanupEvent);
    }

    public RemoveRolesRequest(String selector, CleanupEvent cleanupEvent, Stack stack) {
        super(selector, cleanupEvent);
    }
}
