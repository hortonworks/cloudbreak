package com.sequenceiq.freeipa.flow.freeipa.downscale.event.removehosts;

import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveHostsFromOrchestrationRequest extends AbstractCleanupEvent {

    public RemoveHostsFromOrchestrationRequest(CleanupEvent cleanupEvent) {
        super(cleanupEvent);
    }
}
