package com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate;

import com.sequenceiq.redbeams.flow.stack.RedbeamsEvent;

/**
 * A request for allocating a database server.
 */
public class AllocateDatabaseServerRequest extends RedbeamsEvent {
    public AllocateDatabaseServerRequest(Long stackId) {
        super(stackId);
    }
}
