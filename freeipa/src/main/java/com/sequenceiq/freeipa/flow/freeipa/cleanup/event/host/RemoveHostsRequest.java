package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.host;

import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveHostsRequest extends AbstractCleanupEvent {

    public RemoveHostsRequest(CleanupEvent cleanupEvent, Stack stack, FreeIpa freeIpa) {
        super(cleanupEvent);
    }

    public RemoveHostsRequest(String selector, CleanupEvent cleanupEvent, Stack stack, FreeIpa freeIpa) {
        super(selector, cleanupEvent);
    }
}
