package com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * The event that occurs when a database server has been allocated.
 */
public class AllocateDatabaseServerSuccess extends RedbeamsEvent {

    public AllocateDatabaseServerSuccess(Long resourceId) {
        super(resourceId);
    }

    @Override
    public String toString() {
        return "AllocateDatabaseServerSuccess{"
                + ", resourceId=" + getResourceId()
                + '}';
    }
}
