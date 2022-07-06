package com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * This event occurs when a database server has been successfully deregistered.
 */
public class DeregisterDatabaseServerSuccess extends RedbeamsEvent {

    @JsonCreator
    public DeregisterDatabaseServerSuccess(@JsonProperty("resourceId") Long resourceId) {
        super(resourceId);
    }
}
