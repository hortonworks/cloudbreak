package com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

/**
 * The event that occurs when a database server termination has failed.
 */
public class TerminateDatabaseServerFailed extends RedbeamsFailureEvent {

    public TerminateDatabaseServerFailed(Long resourceId, Exception exception) {
        super(resourceId, exception);
    }

    @Override
    public String toString() {
        return "TerminateDatabaseServerFailed{"
                + "resourceId=" + getResourceId()
                + ", exception=" + getException()
                + '}';
    }
}
