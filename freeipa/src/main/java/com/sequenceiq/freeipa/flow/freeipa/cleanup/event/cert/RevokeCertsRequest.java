package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.cert;

import java.util.Set;

import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RevokeCertsRequest extends AbstractCleanupEvent {

    public RevokeCertsRequest(CleanupEvent cleanupEvent, Stack stack, FreeIpa freeIpa) {
        super(cleanupEvent);
    }

    public RevokeCertsRequest(String selector, CleanupEvent cleanupEvent, Stack stack, FreeIpa freeIpa, Set<String> hosts) {
        super(selector, cleanupEvent);
    }
}
