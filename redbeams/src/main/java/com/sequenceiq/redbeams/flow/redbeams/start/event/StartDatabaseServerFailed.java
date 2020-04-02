package com.sequenceiq.redbeams.flow.redbeams.start.event;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

/**
 * The event that occurs when database server start has failed.
 */
public class StartDatabaseServerFailed extends RedbeamsFailureEvent {

    public StartDatabaseServerFailed(Long resourceId, Exception e) {
        super(resourceId, e);
    }

    @Override
    public String toString() {
        return "StartDatabaseServerFailed{"
                + "resourceId=" + getResourceId()
                + ", exception=" + getException()
                + '}';
    }
}
