package com.sequenceiq.redbeams.flow.redbeams.start.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * This event occurs when a database server has been successfully started.
 */
public class StartDatabaseServerSuccess extends RedbeamsEvent {

    @JsonCreator
    public StartDatabaseServerSuccess(@JsonProperty("resourceId") Long resourceId) {
        super(resourceId);
    }

    @Override
    public String toString() {
        return "StartDatabaseServerSuccess{"
                + "resourceId=" + getResourceId()
                + '}';
    }
}
