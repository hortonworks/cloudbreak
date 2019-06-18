package com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * A request for deregistering a database server after termination.
 */
public class DeregisterDatabaseServerRequest extends RedbeamsEvent {
    public DeregisterDatabaseServerRequest(Long resourceId) {
        super(resourceId);
    }
}
