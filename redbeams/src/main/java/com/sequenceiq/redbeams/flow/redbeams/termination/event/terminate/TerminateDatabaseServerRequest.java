package com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * A request for terminating a database server.
 */
public class TerminateDatabaseServerRequest extends RedbeamsEvent {
    public TerminateDatabaseServerRequest(Long resourceId) {
        super(resourceId);
    }
}
