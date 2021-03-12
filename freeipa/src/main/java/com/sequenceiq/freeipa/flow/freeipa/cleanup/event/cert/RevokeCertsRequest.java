package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.cert;

import java.util.Set;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RevokeCertsRequest extends AbstractCleanupEvent {

    protected RevokeCertsRequest(Long stackId) {
        super(stackId);
    }

    public RevokeCertsRequest(CleanupEvent cleanupEvent, Stack stack) {
        super(cleanupEvent);
    }

    public RevokeCertsRequest(String selector, CleanupEvent cleanupEvent, Stack stack, Set<String> hosts) {
        super(selector, cleanupEvent);
    }
}
