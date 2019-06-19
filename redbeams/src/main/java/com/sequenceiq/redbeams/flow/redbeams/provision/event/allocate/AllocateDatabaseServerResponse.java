package com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public abstract class AllocateDatabaseServerResponse extends RedbeamsEvent {

    protected AllocateDatabaseServerResponse(Long resourceId) {
        super(resourceId);
    }

}
