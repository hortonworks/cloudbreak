package com.sequenceiq.freeipa.flow.freeipa.cleanup.event;

import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;

public abstract class AbstractCleanupEvent extends CleanupEvent {

    /**
     * Need this for Jackson serialization.
     */
    private final CleanupEvent cleanupEvent;

    protected AbstractCleanupEvent(Long stackId) {
        super(stackId);
        this.cleanupEvent = null;
    }

    public AbstractCleanupEvent(CleanupEvent cleanupEvent) {
        super(cleanupEvent.getResourceId(), cleanupEvent.getUsers(), cleanupEvent.getHosts(), cleanupEvent.getRoles(), cleanupEvent.getIps(),
                cleanupEvent.getStatesToSkip(), cleanupEvent.getAccountId(), cleanupEvent.getOperationId(), cleanupEvent.getClusterName(),
                cleanupEvent.getEnvironmentCrn());
        this.cleanupEvent = cleanupEvent;
    }

    public AbstractCleanupEvent(String selector, CleanupEvent cleanupEvent) {
        super(selector, cleanupEvent.getResourceId(), cleanupEvent.getUsers(), cleanupEvent.getHosts(), cleanupEvent.getRoles(), cleanupEvent.getIps(),
                cleanupEvent.getStatesToSkip(), cleanupEvent.getAccountId(), cleanupEvent.getOperationId(), cleanupEvent.getClusterName(),
                cleanupEvent.getEnvironmentCrn());
        this.cleanupEvent = cleanupEvent;
    }

    /**
     * Need this for Jackson serialization. This must be public.
     */
    public CleanupEvent getCleanupEvent() {
        return cleanupEvent;
    }
}
