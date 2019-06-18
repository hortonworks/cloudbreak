package com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * This event occurs when a database server has been successfully deregistered.
 */
public class DeregisterDatabaseServerSuccess extends RedbeamsEvent {
    public DeregisterDatabaseServerSuccess(Long resourceId) {
        super(resourceId);
    }
}
