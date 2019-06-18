package com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * The event that occurs when a database server termination has failed.
 */
public class TerminateDatabaseServerFailed extends RedbeamsEvent {
    public TerminateDatabaseServerFailed(Long resourceId) {
        super(resourceId);
    }
}
