package com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate;

import com.sequenceiq.redbeams.flow.stack.RedbeamsEvent;

/**
 * The event that occurs when a database allocation has failed.
 */
public class AllocateDatabaseServerFailed extends RedbeamsEvent {
    public AllocateDatabaseServerFailed(Long stackId) {
        super(stackId);
    }
}
