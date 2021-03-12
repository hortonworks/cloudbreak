package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.dns;

import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveDnsRequest extends AbstractCleanupEvent {

    protected RemoveDnsRequest(Long stackId) {
        super(stackId);
    }

    public RemoveDnsRequest(CleanupEvent cleanupEvent) {
        super(cleanupEvent);    }

    public RemoveDnsRequest(String selector, CleanupEvent cleanupEvent, Stack stack, FreeIpa freeIpa) {
        super(selector, cleanupEvent);
    }
}
