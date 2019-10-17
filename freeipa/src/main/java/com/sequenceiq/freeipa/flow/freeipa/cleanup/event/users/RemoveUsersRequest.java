package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.users;

import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveUsersRequest extends AbstractCleanupEvent {

    public RemoveUsersRequest(CleanupEvent cleanupEvent, Stack stack, FreeIpa freeIpa) {
        super(cleanupEvent);
    }

    public RemoveUsersRequest(String selector, CleanupEvent cleanupEvent, Stack stack, FreeIpa freeIpa) {
        super(selector, cleanupEvent);
    }
}
