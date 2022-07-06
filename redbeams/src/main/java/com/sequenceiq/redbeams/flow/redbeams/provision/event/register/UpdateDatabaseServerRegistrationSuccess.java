package com.sequenceiq.redbeams.flow.redbeams.provision.event.register;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * This event occurs when a database server has been successfully registered.
 */
public class UpdateDatabaseServerRegistrationSuccess extends RedbeamsEvent {

    @JsonCreator
    public UpdateDatabaseServerRegistrationSuccess(@JsonProperty("resourceId") Long resourceId) {
        super(resourceId);
    }

    @Override
    public String toString() {
        return "UpdateDatabaseServerRegistrationSuccess{"
                + "resourceId=" + getResourceId()
                + '}';
    }
}
