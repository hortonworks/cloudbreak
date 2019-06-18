package com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * The event that occurs when a database server allocation has failed.
 */
public class AllocateDatabaseServerFailed extends RedbeamsEvent {
    public AllocateDatabaseServerFailed(Long resourceId) {
        super(resourceId);
    }
}
