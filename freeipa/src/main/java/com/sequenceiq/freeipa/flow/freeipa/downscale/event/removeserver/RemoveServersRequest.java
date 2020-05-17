package com.sequenceiq.freeipa.flow.freeipa.downscale.event.removeserver;

import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveServersRequest extends AbstractCleanupEvent {

    public RemoveServersRequest(CleanupEvent cleanupEvent) {
        super(cleanupEvent);
    }
}
