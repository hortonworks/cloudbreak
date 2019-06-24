package com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public abstract class TerminateDatabaseServerResponse extends RedbeamsEvent {

    protected TerminateDatabaseServerResponse(Long resourceId) {
        super(resourceId);
    }

}
