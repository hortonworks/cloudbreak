package com.sequenceiq.redbeams.flow.redbeams.start.event;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * This event occurs when a database server has been successfully started.
 */
public class StartDatabaseServerSuccess extends RedbeamsEvent {

    public StartDatabaseServerSuccess(Long resourceId) {
        super(resourceId);
    }

    @Override
    public String toString() {
        return "StartDatabaseServerSuccess{"
                + "resourceId=" + getResourceId()
                + '}';
    }
}
