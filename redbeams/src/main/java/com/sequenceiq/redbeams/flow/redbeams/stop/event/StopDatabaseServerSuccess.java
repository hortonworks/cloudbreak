package com.sequenceiq.redbeams.flow.redbeams.stop.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * This event occurs when a database server has been successfully stopped.
 */
public class StopDatabaseServerSuccess extends RedbeamsEvent {

    @JsonCreator
    public StopDatabaseServerSuccess(@JsonProperty("resourceId") Long resourceId) {
        super(resourceId);
    }
}
