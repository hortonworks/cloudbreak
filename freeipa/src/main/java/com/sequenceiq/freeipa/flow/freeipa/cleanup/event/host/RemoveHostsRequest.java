package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.host;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveHostsRequest extends AbstractCleanupEvent {

    protected RemoveHostsRequest(Long stackId) {
        super(stackId);
    }

    public RemoveHostsRequest(CleanupEvent cleanupEvent, Stack stack) {
        super(cleanupEvent);
    }

    public RemoveHostsRequest(String selector, CleanupEvent cleanupEvent, Stack stack) {
        super(selector, cleanupEvent);
    }
}
