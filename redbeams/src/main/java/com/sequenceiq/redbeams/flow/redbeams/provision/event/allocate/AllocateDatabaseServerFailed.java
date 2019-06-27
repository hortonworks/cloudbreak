package com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

/**
 * The event that occurs when a database server allocation has failed.
 */
public class AllocateDatabaseServerFailed extends RedbeamsFailureEvent {

    public AllocateDatabaseServerFailed(Long resourceId, Exception exception) {
        super(resourceId, exception);
    }

    @Override
    public String toString() {
        return "AllocateDatabaseServerFailed{"
                + "resourceId=" + getResourceId()
                + ", exception=" + getException()
                + '}';
    }
}
