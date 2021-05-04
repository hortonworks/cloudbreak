package com.sequenceiq.freeipa.flow.freeipa.downscale.event.removereplication;

import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveReplicationAgreementsRequest extends AbstractCleanupEvent {

    protected RemoveReplicationAgreementsRequest(Long stackId) {
        super(stackId);
    }

    public RemoveReplicationAgreementsRequest(CleanupEvent cleanupEvent) {
        super(cleanupEvent);
    }
}
