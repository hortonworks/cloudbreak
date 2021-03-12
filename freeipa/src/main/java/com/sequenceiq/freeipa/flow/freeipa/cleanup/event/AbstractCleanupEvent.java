package com.sequenceiq.freeipa.flow.freeipa.cleanup.event;

import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;

public abstract class AbstractCleanupEvent extends CleanupEvent {

    protected AbstractCleanupEvent(Long stackId) {
        super(stackId);
    }

    public AbstractCleanupEvent(CleanupEvent cleanupEvent) {
        super(cleanupEvent.getResourceId(), cleanupEvent.getUsers(), cleanupEvent.getHosts(), cleanupEvent.getRoles(), cleanupEvent.getIps(),
                cleanupEvent.getStatesToSkip(), cleanupEvent.getAccountId(), cleanupEvent.getOperationId(), cleanupEvent.getClusterName(),
                cleanupEvent.getEnvironmentCrn());
    }

    public AbstractCleanupEvent(String selector, CleanupEvent cleanupEvent) {
        super(selector, cleanupEvent.getResourceId(), cleanupEvent.getUsers(), cleanupEvent.getHosts(), cleanupEvent.getRoles(), cleanupEvent.getIps(),
                cleanupEvent.getStatesToSkip(), cleanupEvent.getAccountId(), cleanupEvent.getOperationId(), cleanupEvent.getClusterName(),
                cleanupEvent.getEnvironmentCrn());
    }
}
