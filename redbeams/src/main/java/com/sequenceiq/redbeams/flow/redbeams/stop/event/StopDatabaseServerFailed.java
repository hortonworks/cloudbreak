package com.sequenceiq.redbeams.flow.redbeams.stop.event;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

/**
 * The event that occurs when database server stop has failed.
 */
public class StopDatabaseServerFailed extends RedbeamsFailureEvent {

    public StopDatabaseServerFailed(Long resourceId, Exception e) {
        super(resourceId, e);
    }

    @Override
    public String toString() {
        return "StopDatabaseServerFailed{"
                + "resourceId=" + getResourceId()
                + ", exception=" + getException()
                + '}';
    }
}
