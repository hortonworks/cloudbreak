package com.sequenceiq.redbeams.flow.redbeams.stop.event;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * This event occurs when a database server has been successfully stopped.
 */
public class StopDatabaseServerSuccess extends RedbeamsEvent {

    public StopDatabaseServerSuccess(Long resourceId) {
        super(resourceId);
    }
}
