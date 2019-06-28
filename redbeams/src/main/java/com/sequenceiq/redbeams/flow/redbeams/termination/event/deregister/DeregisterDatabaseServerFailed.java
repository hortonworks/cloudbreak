package com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

/**
 * The event that occurs when database server deregistration has failed.
 */
public class DeregisterDatabaseServerFailed extends RedbeamsFailureEvent {
    public DeregisterDatabaseServerFailed(Long resourceId, Exception e) {
        super(resourceId, e);
    }

    @Override
    public String toString() {
        return "DeregisterDatabaseServerFailed{"
                + "resourceId=" + getResourceId()
                + ", exception=" + getException()
                + '}';
    }
}
