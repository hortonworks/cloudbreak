package com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * The event that occurs when database server deregistration has failed.
 */
public class DeregisterDatabaseServerFailed extends RedbeamsEvent {
    public DeregisterDatabaseServerFailed(Long resourceId) {
        super(resourceId);
    }
}
