package com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * The event that occurs when a database server has been terminated.
 */
public class TerminateDatabaseServerSuccess extends RedbeamsEvent {
    public TerminateDatabaseServerSuccess(Long resourceId) {
        super(resourceId);
    }
}
