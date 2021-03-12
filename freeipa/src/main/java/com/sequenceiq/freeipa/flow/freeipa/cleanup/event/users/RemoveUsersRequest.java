package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.users;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveUsersRequest extends AbstractCleanupEvent {

    protected RemoveUsersRequest(Long stackId) {
        super(stackId);
    }

    public RemoveUsersRequest(CleanupEvent cleanupEvent, Stack stack) {
        super(cleanupEvent);
    }

    public RemoveUsersRequest(String selector, CleanupEvent cleanupEvent, Stack stack) {
        super(selector, cleanupEvent);
    }
}
